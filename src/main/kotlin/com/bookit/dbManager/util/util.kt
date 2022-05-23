package com.bookit.dbManager.util

import com.bookit.dbManager.db.AvailableTime
import com.bookit.dbManager.exceptions.InternalException
import org.springframework.core.env.Environment
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Generate a fixed-length random string token.
 *
 * @param num the initial seed (null is converted to 0).
 * @param lenOfToken length of the string (default 6).
 * @return the random string token.
 */
fun getToken(num: Long?, lenOfToken: Int=6, customAdmChar: List<Char>?=null):String {
    val admissibleChar = customAdmChar ?: ((0..25).map { 'a' + it } + (0..9).map { '0' + it })

    val result = arrayListOf<Char>()
    var seed: Long = num?:0

    for (i in (0..lenOfToken-1)) {
        result.add(admissibleChar[
                Random(seed).nextInt(0,admissibleChar.size-1)
        ])
        seed = Random(seed).nextLong()
    }

    return result.joinToString("")
}

/**
 * A shorthand for converting the OffsetDateTime object to an ISO8601 string.
 *
 * @return the ISO8601 string.
 */
fun OffsetDateTime.toISO() = this.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/**
 * A utility function to generate a random string for Google Meet.
 *
 * @return a random string of the format 'xxx-xxxx-xxx'.
 */
fun randomMeetString(): String {
    val head = List(3) {'a'+Random.nextInt(0,25)}
    val mid = List(4) {'a'+Random.nextInt(0,25)}
    val tail = List(3) {'a'+Random.nextInt(0,25)}

    return head.joinToString("") + "-" + mid.joinToString("") + "-" + tail.joinToString("")
}

/**
 * An implementation for an abstract max() function.
 *
 * @param a the first item.
 * @param b the second item.
 * @return the maximal item between a and b.
 */
fun <T: Comparable<T>> larger(a:T, b:T): T{
    return if (a>b) a else b
}

/**
 * A utility function to merge a list of intervals. An element is assumed to be a Pair<T,T> where the first is smaller than the second.
 *
 * @param list a list of pairs representing intervals where the first item is smaller than or equal to the second. The items are comparable.
 * @return a sorted list of merged intervals
 */
fun <T: Comparable<T>> mergeIntervals(list: List<Pair<T,T>>): List<Pair<T,T>>{
    return list.sortedBy { it.first }.fold(listOf()) { acc, time ->
        assert(time.first<=time.second)

        if (acc.isEmpty())
            listOf(time)
        else {
            val prevStart = acc[acc.size - 1].first
            val prevEnd = acc[acc.size - 1].second

            if (prevEnd >= time.first)
                acc.slice(0 until acc.size - 1) +
                        listOf(
                            Pair(
                                prevStart,
                                larger(prevEnd, time.second)
                            )
                        )
            else
                acc + listOf(time)
        }
    }
}

/**
 * A wrapper of mergeIntervals to handle a list of AvailableTime objects.
 *
 * @param list a list of AvailableTime objects.
 * @return a sorted and merged list of AvailableTime objects.
 */
fun mergeAvailableTime(list: List<AvailableTime>): List<AvailableTime>{
    val unwrapped = list.map { Pair(it.startMinute, it.endMinute) }
    return mergeIntervals(unwrapped).map { AvailableTime(it.first, it.second) }
}

/**
 * A utility function to attach a date and a zone offset to the number of minutes since midnight.
 *
 * @param time minutes since midnight.
 * @param date the date.
 * @param zone the timezone.
 * @return an OffsetDateTime object
 */
fun attachDateToMinute(time: Int, date: LocalDate, zone: ZoneId): OffsetDateTime{
    return ZonedDateTime.of(date, LocalTime.MIDNIGHT.plusMinutes(time.toLong()), zone).toOffsetDateTime()
}

/**
 * A utility function searching for the index of the next interval that has a potential of intersecting [start,end].
 *
 * We assume any interval before the pointer does not intersect [start,end]. We look for the next interval [a,b] such that b>start.
 * @param list the list of intervals.
 * @param pointer the current index.
 * @param start the start value of the compared interval [start,end].
 * @param end the end value of the compared interval [start,end].
 * @return the next index larger than or equal to pointer such that the end value is greater than start.
 */
fun <T : Comparable<T>> getNextInterval(list: List<Pair<T, T>>, pointer: Int, start: T, end: T): Int {
    var p = pointer
    while (p < list.size) {
        if (list[p].second > start) break
        p += 1
    }
    return p
}

/**
 * A shorthand for setting an OffsetDateTime object to certain timezone
 */
fun OffsetDateTime.atZone(zone: ZoneId) = this.atZoneSameInstant(zone).toOffsetDateTime()

/**
 * Decode an encoded availableDays integer into a map that shows the availability of each day of week.
 * @param availableDays the encoded availableDays (see ScheduleType class)
 */
fun getDayOfWeekAvailability(availableDays: Int): Map<DayOfWeek, Boolean> {
    var residue = availableDays
    var result = mapOf<DayOfWeek, Boolean>()
    for (i in 0..6) {
        result = result + Pair(DayOfWeek.SUNDAY.minus(i.toLong()), residue.mod(2) != 0)
        residue /= 2
    }
    return result
}

/**
 * A utility function to check if the availableDays encoding is permitted.
 *
 * Make sure the interger is less than or equal to "1111111".toInt(2) and nonzero.
 * @param encoded the encoded integer
 */
fun validDayOfWeekEncoding(encoded: Int): Boolean {
    return (encoded != 0) && (encoded <= "1111111".toInt(2))
}

/**
 * Ensure an Environment object is non-null. Send proper error message when not.
 *
 * @param env the Environment object.
 */
fun ensureEnvExists(env: Environment?) {
    val errMsg = "Environment is not injected."
    if (env == null)
        throw InternalException(errMsg)
}