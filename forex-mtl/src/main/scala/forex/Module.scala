package forex

import cats.effect._
import cats.syntax.all._
import forex.domain.{Rate, Timestamp}
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._


class Module[F[_]: Async](client: Client[F]) {

  private val initialCache: Map[Rate.Pair, (Rate, Timestamp)] = Map.empty

  // Initialize the cache as F[Ref]
  private val cache: F[Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]] =
    Ref.of[F, Map[Rate.Pair, (Rate, Timestamp)]](initialCache)

  // Use flatMap to unwrap cache before passing it to RatesProgram
  def httpApp: F[HttpApp[F]] = cache.flatMap { unwrappedCache =>
    val ratesService: RatesService[F] = RatesServices.dummy[F](client) // Pass the client
    val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, unwrappedCache)

    val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

    // Directly convert HttpRoutes to HttpApp without using OptionT
    val httpApp: HttpApp[F] = ratesHttpRoutes.orNotFound

    // Return the HttpApp[F] directly
    Async[F].pure(httpApp)
  }
}