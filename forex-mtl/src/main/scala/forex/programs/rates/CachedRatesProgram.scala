package forex.programs.rates

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap._
import forex.domain.TimestampOps.RichTimestamp
import forex.domain._
import forex.programs.rates.errors._
import forex.services.RatesService
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis

import scala.concurrent.duration._

class CachedRatesProgram[F[_]: Sync](ratesService: RatesService[F], jedis: Jedis) extends Algebra[F] {

  // Cache duration in seconds is 4.5 minutes to make sure always have fresh rates within time limit
  private val cacheDurationSeconds = 270.seconds.toSeconds
  private val logger               = LoggerFactory.getLogger(this.getClass)

  private def serializeRate(rate: Rate, timestamp: Timestamp): String = (rate, timestamp).asJson.noSpaces

  private def deserializeRate(data: String): Option[(Rate, Timestamp)] =
    decode[(Rate, Timestamp)](data).toOption

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pairKey = s"${request.from}-${request.to}"
    val now     = TimestampOps.now

    Sync[F]
      .delay {
        Option(jedis.get(pairKey))
      }
      .flatMap {
        case Some(data) =>
          deserializeRate(data) match {
            case Some((rate, timestamp)) if now.isWithinDuration(timestamp, cacheDurationSeconds.seconds) =>
              logger.info(s"Fetched rate for $pairKey from Redis cache: $rate")
              Sync[F].pure(Right(rate)) // Return cached rate if it's still valid

            case _ =>
              fetchAndCacheRate(request.from, request.to, now) // Use `request.from` and `request.to` to form the pair
          }
        case None =>
          fetchAndCacheRate(request.from, request.to, now)
      }
  }

  private def fetchAndCacheRate(from: Currency, to: Currency, now: Timestamp): F[Error Either Rate] = {
    logger.info(s"Fetching rate for $from-$to from API")

    EitherT(ratesService.get(Rate.Pair(from, to)))
      .leftMap(toProgramError)
      .semiflatMap { rate =>
        Sync[F]
          .delay {
            jedis.setex(s"$from-$to", cacheDurationSeconds.toInt, serializeRate(rate, now))
          }
          .flatMap(_ => Sync[F].pure(rate))
      }
      .value
  }
}

object Program {
  def apply[F[_]: Sync](ratesService: RatesService[F], jedis: Jedis): Algebra[F] =
    new CachedRatesProgram[F](ratesService, jedis)
}
