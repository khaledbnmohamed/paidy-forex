package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO].resource.use { client =>
      new Application[IO](client).stream.compile.drain.as(ExitCode.Success)
    }
}

class Application[F[_]: Async](client: Client[F]) {
  def stream: Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](client, config.oneFrame, config.redis)
      httpApp <- Stream.eval(module.httpApp)
      _ <- BlazeServerBuilder[F]
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(httpApp)
            .serve
    } yield ()
}
