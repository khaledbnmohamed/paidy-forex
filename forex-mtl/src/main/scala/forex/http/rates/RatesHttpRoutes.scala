package forex.http.rates

import cats.effect.Async
import cats.data.Validated.Valid
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol, errors => ProgramErrors}
import forex.services.rates.errors.{Error => RatesServiceError}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityEncoder, HttpRoutes}
import org.slf4j.LoggerFactory
import Converters._
import QueryParams._

class RatesHttpRoutes[F[_]: Async](rates: RatesProgram[F]) extends Http4sDsl[F] {

  implicit val programErrorEncoder: EntityEncoder[F, RatesServiceError] = circeEntityEncoder[F, RatesServiceError]
  private val logger = LoggerFactory.getLogger(this.getClass)

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      (from, to) match {
        case (Valid(from), Valid(to)) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Right(rate) =>
          Ok(rate.asGetApiResponse)

        case Left(error) =>
          logger.error(s"Error fetching rates: $error")

          error match {
            case ProgramErrors.Error.ForbiddenError(_) =>
              Forbidden("Missing token")

            case ProgramErrors.Error.NotFoundError(msg) =>
              NotFound(msg)

            case ProgramErrors.Error.RateLookupFailed(msg) =>
              InternalServerError(s"Rate lookup failed: $msg")

            case ProgramErrors.Error.InternalError(_) =>
              InternalServerError("An unexpected error occurred")

            case _ =>
              InternalServerError("An unexpected error occurred")
          }
      }
        case (_, _) => BadRequest("The system doesn't support this currency yet")
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
