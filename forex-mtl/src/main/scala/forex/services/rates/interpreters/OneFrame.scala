package forex.services.rates.interpreters

import cats.effect.Async
import cats.syntax.all._
import forex.domain.{Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Header, Method, Request, Uri}
import org.slf4j.LoggerFactory
import org.typelevel.ci.CIString
class OneFrame[F[_]: Async](client: Client[F]) extends Algebra[F] {

  private val logger = LoggerFactory.getLogger(this.getClass)
  val apiToken: String = sys.env.getOrElse("ONE_FRAME_API_TOKEN", "10dc303535874aeccc86a8251e6992f5")
  private case class OneFrameResponse(from: String, to: String, price: BigDecimal, time_stamp: String)

  // Provide an implicit EntityDecoder and Decoder for OneFrameResponse
  implicit private val oneFrameResponseDecoder: Decoder[OneFrameResponse]      = deriveDecoder[OneFrameResponse]
  implicit private val entityDecoder: EntityDecoder[F, List[OneFrameResponse]] = jsonOf[F, List[OneFrameResponse]]

  // Base URL for OneFrame API
  private val baseUrl = Uri.unsafeFromString("http://localhost:8085/rates")

  // Function to fetch the rate from OneFrame API
  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    val uri = baseUrl.withQueryParam("pair", s"${pair.from}${pair.to}")

    // Log the request URI
    logger.info(s"Requesting OneFrame API with URI: $uri")

    val request = Request[F](Method.GET, uri).putHeaders(Header.Raw(CIString("token"), apiToken))

    client.expect[List[OneFrameResponse]](request).attempt.flatMap {
      case Right(responses) if responses.nonEmpty =>
        // Log the successful response
        logger.info(s"Received response from OneFrame API: $responses")
        val response = responses.head // or handle multiple responses as needed
        val rate     = Rate(pair, Price(response.price), Timestamp.now)
        Async[F].pure(rate.asRight[Error])

      case Right(_) =>
        // Handle the case where the response is empty
        val errorMessage = s"No rates found for pair: ${pair.from}${pair.to}"
        logger.error(errorMessage)
        Async[F].pure(Error.OneFrameLookupFailed(errorMessage).asLeft[Rate])

      case Left(exception) =>
        // Log the exception
        logger.error("Failed to fetch rate from OneFrame", exception)
        val errorMessage = s"Failed to fetch rate from OneFrame: ${exception.getMessage}"
        Async[F].pure(Error.OneFrameLookupFailed(errorMessage).asLeft[Rate])
    }
  }
}