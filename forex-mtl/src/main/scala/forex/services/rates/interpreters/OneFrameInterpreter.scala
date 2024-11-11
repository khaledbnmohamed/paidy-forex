package forex.services.rates.interpreters

import cats.effect.Async
import cats.syntax.all._
import forex.config.OneFrameConfig
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

class OneFrameInterpreter[F[_]: Async](client: Client[F], config: OneFrameConfig) extends Algebra[F] {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val apiToken: String = config.http.token
  private val baseUrl: Uri = Uri.unsafeFromString(s"${config.http.host}:${config.http.port}/rates")

  private case class OneFrameResponse(from: String, to: String, price: BigDecimal, time_stamp: String)

  implicit private val oneFrameResponseDecoder: Decoder[OneFrameResponse]      = deriveDecoder[OneFrameResponse]
  implicit private val entityDecoder: EntityDecoder[F, List[OneFrameResponse]] = jsonOf[F, List[OneFrameResponse]]

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    val uri = baseUrl.withQueryParam("pair", s"${pair.from}${pair.to}")
    val request = Request[F](Method.GET, uri).putHeaders(Header.Raw(CIString("token"), apiToken))

    client.expect[List[OneFrameResponse]](request).attempt.flatMap {
      case Right(responses) if responses.nonEmpty =>
        val response = responses.head
        val rate     = Rate(pair, Price(response.price), Timestamp.now)
        Async[F].pure(rate.asRight[Error])

      case Right(_) =>
        val errorMessage = s"No rates found for pair: ${pair.from}${pair.to}"
        logger.error(errorMessage)
        Async[F].pure(Error.OneFrameLookupFailed(errorMessage).asLeft[Rate])

      case Left(exception) =>
        logger.error("Failed to fetch rate from OneFrame", exception)
        val errorMessage = s"Failed to fetch rate from OneFrame: ${exception.getMessage}"
        Async[F].pure(Error.OneFrameLookupFailed(errorMessage).asLeft[Rate])
    }
  }
}
