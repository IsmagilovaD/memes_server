package service

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.directives.FileInfo
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import config.AppConfig
import domain.{FileExtension, FileToken, MyFileInfo}
import doobie.Transactor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Futures.timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import doobie.implicits._
import org.scalatest.concurrent.ScalaFutures
import pureconfig.ConfigSource

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

class FileServiceSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val config = ConfigSource.default.load[AppConfig] match {
    case Right(cfg) => cfg
    case Left(error) => throw new RuntimeException(s"Failed to load configuration: $error")
  }
  implicit val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    config.db.driver, // Database driver class
    config.db.url, // JDBC URL
    config.db.user, // Database username
    config.db.password // Database password
  )
  private val testFilePath = "/test/file/path"

  private val service = FileService.make(FileServiceTestDB.makeTest(), testFilePath, transactor)

  override def beforeAll(): Unit = {
    val setupIO = FileServiceTestDB.setupTestTable().transact(transactor).unsafeToFuture()
  }

  override def afterAll(): Unit = {
    val cleanupIO = FileServiceTestDB.cleanupTestTable().transact(transactor).unsafeToFuture()
    ScalaFutures.whenReady(cleanupIO) { _ =>
      // Table cleanup completed
    }
  }

  "FileService" should "return Right(MyFileInfo) for valid file extensions" in {
    val metadata = FileInfo("file", "image.jpg", MediaTypes.`image/jpeg`)
    val result = service.checkExtension(metadata)
    result shouldBe a[Right[_, _]]
    result.right.get.token.value should not be empty
    result.right.get.fileExtension.value should not be empty
  }

  it should "return Left(WrongTypeError) for invalid file extensions" in {
    val metadata = FileInfo("file", "document.pdf", MediaTypes.`application/pdf`)
    val result = service.checkExtension(metadata)
    result shouldBe a[Left[_, _]]
    result.left.get.fileName shouldEqual "document.pdf"
  }

  it should "generate a valid destination file path" in {
    val fileInfo = MyFileInfo(FileExtension(".jpg"), FileToken("abcdef"))
    val expectedPath = Paths.get(testFilePath).resolve("abcdef.jpg")
    val result = service.generateDestinationFile(fileInfo)
    result shouldEqual expectedPath
  }

  it should "upload a file info and return the token" in {
    val fileInfo = MyFileInfo(FileExtension(".jpg"), FileToken("abcdef"))
    val expected = "abcdef"
    val result = service.uploadFile(fileInfo)
    ScalaFutures.whenReady(result, timeout(5.seconds)) { actual =>
      actual shouldEqual expected
    }
  }

  it should "return Right(Path) for a valid token" in {
    val fileInfo = MyFileInfo(FileExtension(".jpg"), FileToken("abcdef"))
    // Insert the file info into the database to simulate an existing file
    FileServiceTestDB.makeTest().insertSql(fileInfo).run.transact(transactor).unsafeRunSync()
    val result = service.isValidToken("abcdef")
    result shouldBe a[Right[_, _]]
    result.right.get shouldEqual Paths.get(testFilePath).resolve("abcdef.jpg")
  }

  it should "return Left(FileInfoNotFound) for an invalid token" in {
    val result = service.isValidToken("nonexistent_token")
    result shouldBe a[Left[_, _]]
    result.left.get.token shouldEqual "nonexistent_token"
  }
}
