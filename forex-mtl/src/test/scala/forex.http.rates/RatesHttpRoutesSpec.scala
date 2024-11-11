package forex.http.rates

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol, errors => ProgramErrors}
import org.http4s._
import org.http4s.implicits._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
class RatesHttpRoutesSpec extends AnyFunSuite with Matchers with MockitoSugar {

  private def makeRequest(routes: HttpRoutes[IO], from: String, to: String): Response[IO] = {
    val uri = uri"/rates".withQueryParam("from", from).withQueryParam("to", to)
    val request = Request[IO](method = Method.GET, uri = uri)
    routes.orNotFound.run(request).unsafeRunSync()
  }

  test("should return Ok for a valid request with rate data") {
    val ratesProgram = mock[RatesProgram[IO]]
    val mockRate = Rate(
      pair = Rate.Pair(Currency.USD, Currency.JPY),
      price = Price(0.71),
      timestamp = Timestamp.parse("2024-11-11T09:39:40.458Z")
    )
    val request = RatesProgramProtocol.GetRatesRequest(Currency.USD, Currency.JPY)
    when(ratesProgram.get(request)).thenReturn(IO.pure(Right(mockRate)))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.Ok
  }

  test("should return Forbidden when token is missing") {
    val ratesProgram = mock[RatesProgram[IO]]
    val errorResponse = Left(ProgramErrors.Error.ForbiddenError("Missing token"))

    when(ratesProgram.get(any[RatesProgramProtocol.GetRatesRequest])).thenReturn(IO.pure(errorResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.Forbidden
  }

  test("should return NotFound when rate not available") {
    val ratesProgram = mock[RatesProgram[IO]]
    val errorResponse = Left(ProgramErrors.Error.NotFoundError("Rate not found"))

    when(ratesProgram.get(any[RatesProgramProtocol.GetRatesRequest])).thenReturn(IO.pure(errorResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.NotFound
    response.as[String].unsafeRunSync() shouldBe "\"Rate not found\""
  }

  test("should return InternalServerError when rate lookup fails") {
    val ratesProgram = mock[RatesProgram[IO]]
    val errorResponse = Left(ProgramErrors.Error.RateLookupFailed("Lookup failed"))

    when(ratesProgram.get(any[RatesProgramProtocol.GetRatesRequest])).thenReturn(IO.pure(errorResponse))

    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "USD", "JPY")

    response.status shouldBe Status.InternalServerError
    response.as[String].unsafeRunSync() should include("Rate lookup failed: Lookup failed")
  }

  test("should return BadRequest for unsupported currency") {
    val ratesProgram = mock[RatesProgram[IO]]
    val routes = new RatesHttpRoutes[IO](ratesProgram).routes
    val response = makeRequest(routes, "XYZ", "JPY")

    response.status shouldBe Status.BadRequest
    response.as[String].unsafeRunSync() shouldBe "\"The system doesn't support this currency yet\""
  }
}
