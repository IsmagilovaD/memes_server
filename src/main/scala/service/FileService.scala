package service

import akka.http.scaladsl.server.directives.FileInfo
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import dao.FileInfoSql
import domain.errors.{FileInfoNotFound, WrongTypeError}
import domain.{FileExtension, FileToken, MyFileInfo}
import doobie.WeakAsync.doobieWeakAsyncForAsync
import org.apache.commons.codec.digest.DigestUtils
import doobie._
import doobie.implicits._

import java.nio.file.{Path, Paths}
import scala.concurrent.Future

trait FileService {
  def checkExtension(metadata: FileInfo): Either[WrongTypeError, MyFileInfo]

  def generateToken(fileName: String, extension: String): MyFileInfo

  def generateDestinationFile(myFileInfo: MyFileInfo): Path

  def uploadFile(fileInfo: MyFileInfo): Future[String]

  def isValidToken(token: String): Either[FileInfoNotFound, Path]
}

object FileService {
  private final class FileServiceImpl(sql: FileInfoSql,
                                      fileServerPath: String,
                                      transactor: Transactor[IO]) extends FileService {
    val imageExtensionPattern = """\.(?i)(jpg|jpeg|png|gif|svg)$""".r

    def checkExtension(metadata: FileInfo): Either[WrongTypeError, MyFileInfo] = {
      val fileName = metadata.fileName
      val extension: String = fileName.substring(fileName.lastIndexOf("."))
      imageExtensionPattern.findFirstMatchIn(extension) match {
        case Some(_) =>
          val fileInfo = generateToken(fileName, extension)
          Right(fileInfo)
        case None =>
          Left(WrongTypeError(metadata.fileName))
      }
    }

    def generateToken(fileName: String, extension: String): MyFileInfo = {
      val timestamp = System.currentTimeMillis()
      val uniqueId = s"${fileName}-$timestamp"
      val fileToken = FileToken(DigestUtils.sha256Hex(uniqueId))
      MyFileInfo(FileExtension(extension), fileToken)
    }

    def generateDestinationFile(myFileInfo: MyFileInfo): Path = {
      val filePath = Paths.get(fileServerPath)
      filePath.resolve(s"$filePath\\${myFileInfo.token.value}${myFileInfo.fileExtension.value}")
    }

    def uploadFile(fileInfo: MyFileInfo): Future[String] = {
      sql.insertSql(fileInfo).run.transact(transactor).attempt.map(_ => fileInfo.token.value).unsafeToFuture()
    }

    def isValidToken(token: String): Either[FileInfoNotFound, Path] = {
      sql.findByTokenSql(FileToken(token)).option.transact(transactor).unsafeRunSync() match {
        case Some(fileInfo) =>
          val filePath = Paths.get(fileServerPath)
          Right(filePath.resolve(s"$filePath\\${token}${fileInfo.fileExtension.value}"))
        case None => Left(FileInfoNotFound(token))
      }
    }
  }

  def make(sql: FileInfoSql, fileServerPath: String, transactor: Transactor[IO]): FileService = {
    new FileServiceImpl(sql, fileServerPath, transactor)
  }
}