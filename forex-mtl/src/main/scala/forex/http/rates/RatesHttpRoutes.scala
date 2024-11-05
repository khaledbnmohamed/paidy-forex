package forex.http.rates

import cats.effect.Async
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol, errors => ProgramErrors}
import forex.services.rates.errors.{Error => RatesServiceError}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityEncoder, HttpRoutes}
import org.slf4j.LoggerFactory

class RatesHttpRoutes[F[_]: Async](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  implicit val programErrorEncoder: EntityEncoder[F, RatesServiceError] = circeEntityEncoder[F, RatesServiceError]
  private val logger = LoggerFactory.getLogger(this.getClass)

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Right(rate) =>
          Ok(rate.asGetApiResponse)

        case Left(error) =>
          logger.error(s"Error fetching rates: $error")

          error match {
            case ProgramErrors.Error.RateLookupFailed(message) if message.toLowerCase.contains("forbidden") =>
              Forbidden("Missing token")

            case ProgramErrors.Error.RateLookupFailed(message) if message.toLowerCase.contains("not found") =>
              NotFound(s"Rate lookup failed: $message")

            case ProgramErrors.Error.RateLookupFailed(message) =>
              InternalServerError(s"Rate lookup failed: $message")

            case _ =>
              InternalServerError("An unexpected error occurred")
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
