
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.rates.Protocol.GetApiResponse
import forex.http.rates.RatesHttpRoutes
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol, errors => ProgramErrors}
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.implicits._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
class RatesHttpRoutesSpec extends AnyFunSuite with Matchers with MockitoSugar {

  private val mockRate = Rate(
    pair = Rate.Pair(Currency.USD, Currency.JPY),
    price = Price(0.50691912186796811),
    timestamp = Timestamp.now
  )
  private def makeRequest(routes: HttpRoutes[IO], from: String, to: String): Response[IO] = {
    val uri = uri"/rates".withQueryParam("from", from).withQueryParam("to", to)
    val request = Request[IO](method = Method.GET, uri = uri)
    routes.orNotFound.run(request).unsafeRunSync()
  }

  test("should return Ok for a valid request with rate data") {
    val ratesProgram = mock[RatesProgram[IO]]
    val request = RatesProgramProtocol.GetRatesRequest(Currency.USD, Currency.JPY)
    val rateResponse = Right(mockRate)

    when(ratesProgram.get(request)).thenReturn(IO.pure(rateResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.Ok
    val expectedResponse = GetApiResponse(
      from = mockRate.pair.from,
      to = mockRate.pair.to,
      price = mockRate.price,
      timestamp = mockRate.timestamp
    )
    response.as[GetApiResponse].unsafeRunSync() shouldBe expectedResponse
  }

  test("should return Forbidden when token is missing") {
    val ratesProgram = mock[RatesProgram[IO]]
    val errorResponse = Left(ProgramErrors.Error.ForbiddenError("Missing token"))

    when(ratesProgram.get(any[RatesProgramProtocol.GetRatesRequest])).thenReturn(IO.pure(errorResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.Forbidden
    response.as[String].unsafeRunSync() shouldBe "Missing token"
  }

  test("should return NotFound when rate not available") {
    val ratesProgram = mock[RatesProgram[IO]]
    val errorResponse = Left(ProgramErrors.Error.NotFoundError("Rate not found"))

    when(ratesProgram.get(any[RatesProgramProtocol.GetRatesRequest])).thenReturn(IO.pure(errorResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.NotFound
    response.as[String].unsafeRunSync() shouldBe "Rate not found"
  }
}
