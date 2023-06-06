import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import cats.effect.IO
import config.AppConfig
import controller.FileController
import dao.FileInfoSql
import doobie.Transactor
import pureconfig.ConfigSource
import service.FileService

import scala.util.{Failure, Success}

object FileServer extends App {
  implicit val system = ActorSystem("file-server")
  implicit val executionContext = system.dispatcher
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
  implicit val filePath: String = config.files.path
  val fileInfoSql = FileInfoSql.make()
  val fileService = FileService.make(fileInfoSql, filePath, transactor)
  val fileController = FileController.make(fileService)
  val routes = fileController.routes

  val futureBinding = Http().newServerAt(config.server.host, config.server.port).bind(routes)
  futureBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
    case Failure(ex) =>
      system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
      system.terminate()
  }
}
