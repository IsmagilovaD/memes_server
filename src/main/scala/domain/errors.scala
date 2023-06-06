package domain

import cats.syntax.option._

object errors {
  sealed abstract class AppError(
                                  val message: String,
                                  val cause: Option[Throwable] = None
                                )

  case class FileInfoNotFound(token: String)
    extends AppError(s"File with token ${token} not found")

  case class FileSaveError(fileName: String, cause0: Throwable)
    extends AppError(s"An error occurred while saving  file with name ${fileName}", cause0.some)

  case class WrongTypeError(fileName: String)
    extends AppError(s"Wrong content type for file with name ${fileName}." +
      s" Only image memes(jpg, jpeg, png, gif, svg).")

  case class InternalError(cause0: Throwable)
    extends AppError("Internal error", cause0.some)

  case class MockError(override val message: String) extends AppError(message)
}
