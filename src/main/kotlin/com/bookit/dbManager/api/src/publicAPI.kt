package com.bookit.dbManager.api.src

import com.bookit.dbManager.db.BackendUser
import com.bookit.dbManager.db.LockedSlot
import com.bookit.dbManager.db.ScheduleType
import com.bookit.dbManager.util.*
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Handles /api/get_timeslot API calls.
 */
fun getTimeslot(
    pageLimit: Int,
    dayLimit: Int,
    startLocalDate: LocalDate,
    user: BackendUser,
    scheduleType: ScheduleType,
    lockedSlots: List<LockedSlot>
): List<Pair<OffsetDateTime, OffsetDateTime>> {
    val zone = scheduleType.zoneId
    val duration = scheduleType.duration
    val timeBetweenSlots = scheduleType.timeBetweenSlots
    assert(validDayOfWeekEncoding(scheduleType.availableDays))
    val dayOfWeekAvailability = getDayOfWeekAvailability(scheduleType.availableDays)

    // Merge time periods.
    val availableList = mergeIntervals(scheduleType.availableList.map { Pair(it.startMinute, it.endMinute) })
        .filter { (it.second - it.first) >= duration } // filter out available periods that are shorter than the session duration
    val busyPeriods = mergeIntervals(user.busyPeriods
        .map { Pair(it.startTime.atZone(zone), it.endTime.atZone(zone)) })
    val lockedPeriod = mergeIntervals(lockedSlots
        .filter {
            it.createdAt.plusMinutes(it.expiration.toLong()).isAfter(OffsetDateTime.now())
        } // filter out expired records
        .map { Pair(it.startTime.atZone(zone), it.endTime.atZone(zone)) })

    // No availability.
    if (availableList.isEmpty()) return listOf<Nothing>()

    // Initialize variables.
    var timeslots = arrayListOf<Pair<OffsetDateTime, OffsetDateTime>>()
    val startDateTime = attachDateToMinute(availableList[0].first, startLocalDate, zone)
    var dateOffset = 0
    var availableListPointer = 0
    var busyPeriodPointer = 0
    var bookedPeriodPointer = 0

    /**
     * move dateOffset until the day of week is available.
     */
    fun getNextAvailableDay() {
        while (!dayOfWeekAvailability[
                    startLocalDate.plusDays(dateOffset.toLong()).dayOfWeek]!!
        ) {
            dateOffset += 1
        }
    }
    getNextAvailableDay()
    var currentStart = startDateTime.plusDays(dateOffset.toLong())
    var currentEnd = currentStart.plusMinutes(duration.toLong())

    /**
     * the utility function to update the pointer and return the next [start,end] interval that falls within availability.
     *
     * It updates the variable availableListPointer and DateOffset. It also follows the following logic:
     * 1. move start and end to the next slot location.
     * 2. turn "end" into number of minutes after midnight. If it is more than the end of the current interval => update is needed.
     * 3. move pointer by one (update DateOffset if necessary).
     * 4. get the next [start,end] according to the current available time slot.
     */
    fun getNextSlot() {
        var newStart = currentEnd.plusMinutes(timeBetweenSlots.toLong())
        var newEnd = newStart.plusMinutes(duration.toLong())

        if (newEnd.hour * 60 + newEnd.minute > availableList[availableListPointer].second) {
            dateOffset += (availableListPointer + 1) / availableList.size
            availableListPointer = (availableListPointer + 1).mod(availableList.size)
            // If date change happens, increase the dateOffset until the day of week is available.
            if (availableListPointer == 0) {
                getNextAvailableDay()
            }
            newStart = attachDateToMinute(
                availableList[availableListPointer].first,
                startLocalDate.plusDays(dateOffset.toLong()),
                zone
            )
            newEnd = newStart.plusMinutes(duration.toLong())
        }
        currentStart = newStart
        currentEnd = newEnd
    }

    // The core code of the timeslot generation begins here.

    while (currentStart < startDateTime.plusDays(dayLimit.toLong())) {
        busyPeriodPointer = getNextInterval(busyPeriods, busyPeriodPointer, currentStart, currentEnd)
        bookedPeriodPointer = getNextInterval(lockedPeriod, bookedPeriodPointer, currentStart, currentEnd)
        // Check the intersection with busy and booked periods
        if ((busyPeriodPointer >= busyPeriods.size || !busyPeriods[busyPeriodPointer].first.isBefore(currentEnd)) && // no overlap with busyPeriod
            (bookedPeriodPointer >= lockedPeriod.size || !lockedPeriod[bookedPeriodPointer].first.isBefore(
                currentEnd
            ))
        ) // no overlap with bookedPeriod
        {
            timeslots.add(Pair(currentStart, currentEnd))
            if (timeslots.size >= pageLimit) break
        }
        getNextSlot()

    }

    return timeslots.toList()
}