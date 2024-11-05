package forex

import cats.effect.{ Async, Ref }
import forex.domain.{ Rate, Timestamp }
import forex.services.RatesService

package object programs {
  type RatesProgram[F[_]] = rates.Algebra[F]

  final val RatesProgram = rates.Program

  def createRatesProgram[F[_]: Async](ratesService: RatesService[F],
                                      cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]): RatesProgram[F] =
    new rates.Program[F](ratesService, cache)
}
