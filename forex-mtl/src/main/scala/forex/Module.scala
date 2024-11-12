package forex

import cats.effect.{ Async, Resource }
import forex.config._
import forex.http.rates.RatesHttpRoutes
import forex.programs.rates.{ Algebra, CachedRatesProgram }
import forex.services.{ RatesService, RatesServices }
import org.http4s.{ HttpApp, HttpRoutes }
import org.http4s.client.Client
import redis.clients.jedis.Jedis

class Module[F[_]: Async](client: Client[F], config: OneFrameConfig, redisConfig: RedisConfig) {

  private def jedisResource: Resource[F, Jedis] =
    Resource.make {
      Async[F].delay(new Jedis(redisConfig.host, redisConfig.port))
    } { jedis =>
      Async[F].delay(jedis.close())
    }

  def httpApp: F[HttpApp[F]] = jedisResource.use { jedis =>
    val ratesService: RatesService[F] = RatesServices.dummy[F](client, config)
    val ratesProgram: Algebra[F]      = new CachedRatesProgram[F](ratesService, jedis)

    val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes
    val httpApp: HttpApp[F]            = ratesHttpRoutes.orNotFound

    Async[F].pure(httpApp)
  }
}
