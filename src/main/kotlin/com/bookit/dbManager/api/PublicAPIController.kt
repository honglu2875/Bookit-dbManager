package com.bookit.dbManager.api

import com.bookit.dbManager.db.*
import com.bookit.dbManager.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.env.Environment
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.annotation.Resource

@SpringBootApplication
@RestController
class PublicAPIController @Autowired constructor(
    val bookedSlotRepository: BookedSlotRepository,
    val backendUserRepository: BackendUserRepository,
    val scheduleTypeRepository: ScheduleTypeRepository,
    val apiAuthRepository: ApiAuthRepository
) {
    val log = logger<SecuredAPIController>()

    @Resource
    private val env: Environment? = null

    /**
     * /api
     *
     * Return a welcome string.
     */
    @GetMapping("/api")
    fun api(model: Model): Any{
        return successGenerator("Welcome. This is a public endpoint.")
    }

    /**
     * /api/get_timeslot
     *
     * TODO: More thorough tests!
     * A public api to obtain a list of available slots according to the schedule type and the calendar availability.
     * A timeslot is generated sequentially in each available time period described in ScheduleType.availableTime.
     * It goes as far as 3 months from the starting date for every single request.
     * The api only uses the latest synced data without sending queries to Google Calendar. If the fresh data is required, please call /api/backend/force_sync beforehand.
     * @param email the calendar owner's email address.
     * @param token the string token corresponding to the schedule type.
     * @param limit (optional, default=100) the maximum amount of timeslots to return (max=100).
     * @param startDate (optional, default:today) the starting date (with format YYYY-mm-dd).
     */
    @GetMapping("/api/get_timeslot")
    fun get_timeslot(email: String, token: String, limit: Int?, startDate: String?): Any {
        val pageLimit = limit ?: 100
        val startLocalDate = if (startDate == null) LocalDate.now() else LocalDate.parse(startDate)
        val user = getUser(email)
        val scheduleType = getScheduleType(token)
        val bookedSlots: List<BookedSlot> = bookedSlotRepository.findAllByHost(user)

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
        val bookedPeriod = mergeIntervals(bookedSlots
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

        while (currentStart < startDateTime.plusMonths(3)) {
            busyPeriodPointer = getNextInterval(busyPeriods, busyPeriodPointer, currentStart, currentEnd)
            bookedPeriodPointer = getNextInterval(bookedPeriod, bookedPeriodPointer, currentStart, currentEnd)
            // Check the intersection with busy and booked periods
            if ((busyPeriodPointer >= busyPeriods.size || !busyPeriods[busyPeriodPointer].first.isBefore(currentEnd)) && // no overlap with busyPeriod
                (bookedPeriodPointer >= bookedPeriod.size || !bookedPeriod[bookedPeriodPointer].first.isBefore(
                    currentEnd
                ))
            ) // no overlap with bookedPeriod
            {
                timeslots.add(Pair(currentStart, currentEnd))
                if (timeslots.size >= pageLimit) break
            }
            getNextSlot()

        }

        return timeslots

    }


    fun getScheduleType(scheduleTypeToken: String): ScheduleType{
        val errMsg = "ScheduleType not found."
        return scheduleTypeRepository.findByToken(scheduleTypeToken)
            ?: throw Exception(errMsg)
    }

    fun getUser(email: String): BackendUser{
        val errMsg = "User not found."
        return backendUserRepository.findByEmail(email)
            ?: throw Exception(errMsg)
    }
}

