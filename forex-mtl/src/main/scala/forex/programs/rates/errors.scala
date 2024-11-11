package forex.programs.rates

import forex.services.rates.errors.{Error => RatesServiceError}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object errors {

  sealed trait Error extends Exception

  object Error {
    implicit val encoder: Encoder[Error] = deriveEncoder[Error]
    final case class RateLookupFailed(msg: String) extends Error
    final case class ForbiddenError(msg: String) extends Error
    final case class NotFoundError(msg: String) extends Error
    final case class InternalError(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg)
      if msg.toLowerCase.contains("forbidden") =>
      Error.ForbiddenError("Missing token")

    case RatesServiceError.OneFrameLookupFailed(msg) if msg.toLowerCase.contains("not found") =>
      Error.NotFoundError(s"Rate lookup failed: $msg")

    case RatesServiceError.OneFrameLookupFailed(msg) =>
      Error.RateLookupFailed(msg)

    case _ =>
      Error.InternalError("Something went wrong")
  }
}
