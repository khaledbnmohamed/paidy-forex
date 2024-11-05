package forex.programs.rates

import forex.domain.{ Currency, Timestamp }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      rate: BigDecimal,
      from: Currency,
      to: Currency,
      timestamp: Timestamp
  )

  // Ensure encoders for Currency and Timestamp are available
  implicit val currencyEncoder: Encoder[Currency]   = deriveEncoder[Currency]
  implicit val timestampEncoder: Encoder[Timestamp] = deriveEncoder[Timestamp]

  // Deriving Encoder for GetApiResponse using Circe
  implicit val getApiResponseEncoder: Encoder[GetApiResponse] = deriveEncoder[GetApiResponse]

  // Providing EntityEncoder for GetApiResponse
  implicit def getApiResponseEntityEncoder[F[_]]: EntityEncoder[F, GetApiResponse] =
    jsonEncoderOf[F, GetApiResponse]
}
