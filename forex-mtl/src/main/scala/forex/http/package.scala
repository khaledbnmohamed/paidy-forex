package forex

import cats.effect.Concurrent
import io.circe.generic.extras.decoding.{EnumerationDecoder, UnwrappedDecoder}
import io.circe.generic.extras.encoding.{EnumerationEncoder, UnwrappedEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe._

package object http {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  // Change Sync to Concurrent for F[_] to ensure the implicit is available
  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Concurrent]: EntityDecoder[F, A] = jsonOf[F, A]
}
