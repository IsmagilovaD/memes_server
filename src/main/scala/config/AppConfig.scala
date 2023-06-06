package config

import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader


final case class AppConfig(db: DbConfig, server: ServerConfig, files: FilesConfig)

object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader

  def load: IO[AppConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[AppConfig])
}

final case class DbConfig(
                           url: String,
                           driver: String,
                           user: String,
                           password: String
                         )

object DbConfig {
  implicit val reader: ConfigReader[DbConfig] = deriveReader
}

final case class ServerConfig(host: String, port: Int)

object ServerConfig {
  implicit val reader: ConfigReader[ServerConfig] = deriveReader
}

final case class FilesConfig(path: String)

object FilesConfig {
  implicit val reader: ConfigReader[FilesConfig] = deriveReader
}

