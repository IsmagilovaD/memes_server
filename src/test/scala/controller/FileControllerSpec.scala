package controller

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes, Multipart, StatusCodes}
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akka.util.ByteString
import domain.{FileExtension, FileToken, MyFileInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.FileService
import spray.json.{JsObject, JsString}

import java.nio.file.Paths
import scala.concurrent.Future

class FileControllerSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  //   Create a test file for uploading
  val testFilePath = ("controller/resources/exampleJpeg.jpg")

  // Create a mock FileService
  val mockService = mock[FileService]

  // Create the FileControllerImpl instance
  val fileController = FileController.make(mockService)

  "FileControllerImpl" should {
    "return OK and call uploadFile method when uploading a file" in {
      // Define test data
      val mockFileInfo = FileInfo("file", "example.jpg", MediaTypes.`image/jpeg`)
      val mockToken = FileToken("abcdef")
      val mockMyFileInfo = MyFileInfo(FileExtension(".jpg"), mockToken)
      val mockResponse = "Upload successful"

      // Mock the service methods
      when(mockService.checkExtension(any[FileInfo])).thenReturn(Right(mockMyFileInfo))
      when(mockService.generateDestinationFile(any[MyFileInfo])).thenReturn(Paths.get(".src/test/scala/controller/uploaded"))
      when(mockService.uploadFile(any[MyFileInfo])).thenReturn(Future.successful(mockResponse))

      // Perform the request
      Post("/upload", Multipart.FormData(Source.single(
        Multipart.FormData.BodyPart.Strict(
          "file",
          HttpEntity(testFilePath),
          Map("filename" -> mockFileInfo.fileName)
        )
      ))) ~> fileController.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual mockResponse
      }
    }

    "return OK and the file content when requesting a valid file" in {
      import domain.JsonSupport
      // Define test data
      val mockToken = "abc123"
      val mockFilePath = Paths.get("src/test/scala/controller/resources/exampleJpeg.jpg")
      val mockFileData = ByteString("file content")
      val mockContentType = MediaTypes.`image/jpeg`

      // Mock the service methods
      when(mockService.isValidToken(mockToken)).thenReturn(Right(mockFilePath))

      // Convert TokenRequest to JSON manually
      val tokenRequestJson = JsObject("token" -> JsString(mockToken))
      val jsonRequestEntity = HttpEntity(ContentTypes.`application/json`, tokenRequestJson.toString)

      // Perform the request
      Post("/file", jsonRequestEntity) ~> fileController.routes ~> check {
        status shouldEqual StatusCodes.OK
        contentType shouldEqual mockContentType.toContentType
      }
    }
  }
}

