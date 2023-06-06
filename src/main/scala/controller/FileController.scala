package controller

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, fileUpload, get, onComplete, onSuccess, parameters, path, pathPrefix, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import domain.errors.{FileInfoNotFound, FileSaveError}
import domain.{JsonSupport, TokenRequest}
import service.FileService

import scala.util.{Failure, Success}

trait FileController {
  def routes: Route
}

object FileController {
  case class FileControllerImpl(service: FileService)(implicit val materializer: Materializer)
    extends FileController with JsonSupport {

    val fileRoute: Route = concat(
      path("upload") {
        post {
          fileUpload("file") {
            case (metadata, fileStream) =>
              uploadFile(metadata, fileStream)
          }
        }
      },
      path("file") {
        post {
          entity(as[TokenRequest]) { fileToken =>
            val token = fileToken.token
            getFile(token)
          }
        }
      }
    )


    def uploadFile(metadata: FileInfo, fileStream: Source[ByteString, Any]): Route = {
      val fileInfo = service.checkExtension(metadata)
      fileInfo match {
        case Right(fI) =>
          val destinationFile = service.generateDestinationFile(fI)
          val fileSavedFuture = fileStream.runWith(FileIO.toPath(destinationFile))
          onSuccess(fileSavedFuture) { result =>
            result.status match {
              case Success(_) => onSuccess(service.uploadFile(fI)) {
                token => complete(TokenRequest(token))
              }
              case Failure(exception) => complete(StatusCodes.InternalServerError,
                FileSaveError(metadata.fileName, exception).message)
            }
          }
        case Left(th) => complete(StatusCodes.BadRequest, th.message)
      }
    }

    def getFile(token: String): Route = {
      service.isValidToken(token) match {
        case Left(exception) => complete(StatusCodes.NotFound, exception.message)
        case Right(value) => {
          val fileSource = FileIO.fromPath(value)
          val checkResult = fileSource.runFold(None: Option[Unit]) { (_, _) =>
            Some(())
          }
          onComplete(checkResult) {
            case Success(_) =>
              val extension = value.toString.substring(value.toString.lastIndexOf("."))
              val responseEntity = HttpEntity(getMediaType(extension), fileSource)
              val response = HttpResponse(status = StatusCodes.OK, entity = responseEntity)
              complete(response)
            case _ =>
              complete(StatusCodes.NotFound, FileInfoNotFound(token).message)
          }
        }
      }
    }

    def getMediaType(fileExtension: String): ContentType =
      fileExtension.toLowerCase match {
        case ".jpg" | ".jpeg" => MediaTypes.`image/jpeg`
        case ".png" => MediaTypes.`image/png`
        case ".gif" => MediaTypes.`image/gif`
        case ".svg" => MediaTypes.`image/svg+xml`
        case _ => ContentTypes.`application/octet-stream`
      }

    def routes: Route = fileRoute
  }

  def make(service: FileService)(implicit materializer: Materializer): FileController = FileControllerImpl(service)(materializer)
}
