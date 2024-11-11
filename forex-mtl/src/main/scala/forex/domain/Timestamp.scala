package forex.domain

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def parse(dateTimeString: String): Timestamp =
    Timestamp(OffsetDateTime.parse(dateTimeString))
}

object TimestampOps {
  implicit class RichTimestamp(timestamp: Timestamp) {
    def isWithinDuration(other: Timestamp, duration: FiniteDuration): Boolean = {
      val elapsedTime = java.time.Duration.between(other.value.toInstant, timestamp.value.toInstant)
      elapsedTime.toMillis < duration.toMillis // Check if the elapsed time is less than the duration in milliseconds
    }
  }

  def now: Timestamp =
    Timestamp(OffsetDateTime.now)
}
