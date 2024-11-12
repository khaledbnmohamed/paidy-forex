package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
  http: HttpConfig,
  oneFrame: OneFrameConfig,
  redis: RedisConfig
)
case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class HttpOneFrameConfig(
                               host: String,
                               port: Int,
                               token: String
                             )
case class OneFrameConfig(http: HttpOneFrameConfig)

case class RedisConfig(host: String, port: Int, expiry: FiniteDuration)