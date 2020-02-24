package sharedbudget

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

object Utils {
    fun firstDayOfMonth(instant: Instant = Instant.now()): Instant = YearMonth.from(instant.atZone(UTC_ZONE))
        .atDay(1)
        .atStartOfDay(UTC_ZONE)
        .toInstant()

    private val UTC_ZONE = ZoneId.of("UTC")
}