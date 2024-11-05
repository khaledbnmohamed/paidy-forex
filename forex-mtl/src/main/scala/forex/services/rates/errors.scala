package forex.services.rates

import io.circe.{Encoder, Json}

object errors {

  sealed trait Error

  object Error {
    case class OneFrameLookupFailed(message: String) extends Error

    // Define an implicit encoder for Error
    implicit val encoder: Encoder[Error] = new Encoder[Error] {
      def apply(e: Error): Json = e match {
        case OneFrameLookupFailed(message) =>
          Json.obj(
            "errorType" -> Json.fromString("OneFrameLookupFailed"),
            "message" -> Json.fromString(message)
          )
      }
    }
  }
}
