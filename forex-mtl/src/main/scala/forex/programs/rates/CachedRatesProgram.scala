package forex.programs.rates

import cats.data.EitherT
import cats.effect.{ Ref, Sync }
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.domain.TimestampOps.RichTimestamp
import forex.domain._
import forex.programs.rates.errors._
import forex.services.RatesService
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class CachedRatesProgram[F[_]: Sync](ratesService: RatesService[F], cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]])
    extends Algebra[F] {

  private val cacheDuration = 5.minutes
  private val logger        = LoggerFactory.getLogger(this.getClass)

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pair = Rate.Pair(request.from, request.to)
    val now  = TimestampOps.now

    // Check cache first
    for {
      cacheData <- cache.get
      result <- cacheData.get(pair) match {
                 case Some((rate, timestamp)) if now.isWithinDuration(timestamp, cacheDuration) =>
                   logger.info(s"Fetched rate for $pair from cache: $rate")

                   Sync[F].pure(Right(rate)) // Return cached rate if it's still valid
                 case _ =>
                   // Fetch new rate and update cache
                   logger.info(s"Fetching rate for $pair from API")

                   EitherT(ratesService.get(pair))
                     .leftMap(toProgramError)
                     .semiflatMap { rate =>
                       cache.update(_ + (pair -> (rate, now))).as(rate)
                     }
                     .value
               }
    } yield result
  }
}

object Program {
  def apply[F[_]: Sync](
      ratesService: RatesService[F],
      cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]
  ): Algebra[F] = new CachedRatesProgram[F](ratesService, cache)
}
