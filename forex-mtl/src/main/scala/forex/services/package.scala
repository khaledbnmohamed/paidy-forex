package forex

import cats.effect.Async
import org.http4s.client.Client

package object services {
  // Define RatesService as an alias for the Algebra type in rates
  type RatesService[F[_]] = rates.Algebra[F]

  // Provide a concrete implementation for the RatesService
  object RatesServices {
    // Dummy interpreter that satisfies the RatesService type
    def dummy[F[_]: Async](client: Client[F]): RatesService[F] =
      rates.Interpreters.dummy[F](client)
  }
}
