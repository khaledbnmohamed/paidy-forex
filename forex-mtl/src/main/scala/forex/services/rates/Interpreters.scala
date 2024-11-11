package forex.services.rates

import cats.effect.Async
import forex.config.OneFrameConfig
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Async](client: Client[F], config: OneFrameConfig): Algebra[F] =
    new OneFrameInterpreter[F](client, config)
}