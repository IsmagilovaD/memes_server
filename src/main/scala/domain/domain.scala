import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import doobie.util.Read
import spray.json.DefaultJsonProtocol


package object domain {
  case class FileExtension(value: String)

  object FileExtension {
    implicit val doobieRead: Read[FileExtension] = Read[String].map(FileExtension(_))
  }

  case class FileToken(value: String)

  object FileToken extends DefaultJsonProtocol {
    implicit val doobieRead: Read[FileToken] = Read[String].map(FileToken(_))

  }

  case class TokenRequest(token: String)

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val tokenRequestFormat = jsonFormat1(TokenRequest)
  }
}