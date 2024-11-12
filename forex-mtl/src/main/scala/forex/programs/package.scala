package forex

import cats.effect.Async
import forex.services.RatesService
import redis.clients.jedis.Jedis

package object programs {
  type RatesProgram[F[_]] = rates.Algebra[F]

  final val RatesProgram = rates.Program

  def createRatesProgram[F[_]: Async](
                                       ratesService: RatesService[F],
                                       jedis: Jedis
                                     ): RatesProgram[F] =
    new rates.CachedRatesProgram[F](ratesService, jedis)
}
