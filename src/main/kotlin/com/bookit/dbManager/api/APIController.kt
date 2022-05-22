package com.bookit.dbManager.api

import com.bookit.dbManager.db.*
import com.bookit.dbManager.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.annotation.Resource
import kotlin.math.min

@SpringBootApplication
@RestController
class APIController @Autowired constructor(val bookedSlotRepository: BookedSlotRepository,
                                           val backendUserRepository: BackendUserRepository,
                                           val scheduleTypeRepository: ScheduleTypeRepository,
                                           val apiAuthRepository: ApiAuthRepository)
{
    val log = logger<APIController>()
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
     * /api/backend/welcome
     *
     * Does nothing but hiding behind an authorization filter. Return a welcome string if authorized.
     */
    @PostMapping("/api/backend/welcome")
    fun welcome(): Any{
        return successGenerator("Welcome. Your authorization token is correct.")
    }

    /**
     * /api/backend/update_refresh_token
     *
     * Update the refresh token of a backend user. It reads a JSON payload.
     * Must have: email, refresh_token
     * @param body A JSON string.
     */
    @PostMapping("/api/backend/update_refresh_token")
    fun update_refresh_token(@RequestBody body: UpdateRefreshToken
    ): Any {
        ensureEnvExists(env)

        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")

        val user: BackendUser = getUser(body.email)

        if (checkRefreshToken(clientId, clientSecret, body.refreshToken, log=log)) {
            val updatedUser = user.updateRefreshToken(body.refreshToken)
            backendUserRepository.save(updatedUser)
        }
        else
            return errorGenerator("Invalid refresh token.", HttpStatus.BAD_REQUEST, log=log)

        return successGenerator("Ok.", log=log)
    }

    /**
     * /api/backend/add_user
     *
     * Add a backend user. It reads a JSON payload. See the docs for the object BackendUser.
     * The data should not contain: createdAt, busyPeriods.
     * Must have: email, refreshToken.
     * @param body A JSON string.
     */
    @PostMapping("/api/backend/add_user")
    fun add_user(@RequestBody body: AddBackendUser): Any {
        ensureUserNonexist(body.email)
        backendUserRepository.save(BackendUser(body))
        return successGenerator("Ok.", log=log)
    }

    /**
     * /api/backend/add_schedule_type
     *
     * Add a new schedule type for a backend user.
     * The body is a JSON string containing fields: email, duration, description, availableDays (for encoding rule, see docs on class ScheduleType).
     *
     * @param body A JSON string.
     */
    @PostMapping("/api/backend/add_schedule_type")
    fun add_schedule_type(@RequestBody body: AddScheduleType): Any {
        val user: BackendUser = getUser(body.email)
        if (validDayOfWeekEncoding(body.availableDays)) return errorGenerator(
            "Invalid days encoding.",
            HttpStatus.BAD_REQUEST,
            log = log
        )
        scheduleTypeRepository.save(ScheduleType(body, user))
        return successGenerator("Ok.", log = log)
    }

    /**
     * /api/backend/change_available_time
     *
     * Update the available time periods for a given ScheduleType.
     * The body is a JSON array describing available time periods which completely replace the original one in the specified ScheduleType.
     *
     * @param token the token identifying the ScheduleType
     * @param body A JSON array.
     */
    @PostMapping("/api/backend/change_available_time")
    fun change_available_time(token: String, @RequestBody body: List<AvailableTime>): Any{
        val scheduleType = getScheduleType(token)
        scheduleTypeRepository.save(scheduleType.updateAvailableTime(body))
        return successGenerator("Ok.", log=log)
    }

    /**
     * /api/backend/add_event
     *
     * Add a new event to a backend user's default calendar (calendarId=email). The body is a JSON string containing:
     * hostEmail, startTime, endTime, attendees (another JSON array whose content is a JSON object with email and name fields), description
     * If the response from Google contains "status" key and the value is "confirmed", a record of the event will be persisted in the database.
     * @param body a JSON string.
     * @return the original JSON string response from Google Calendar API.
     */
    @PostMapping("/api/backend/add_event")
    fun add_event(@RequestBody body: AddBookedSlot): Any {
        val user: BackendUser = getUser(body.hostEmail)
        ensureEnvExists(env)

        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")!!
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")!!
        val apiKey = env.getProperty("api.provider.google.key")!!
        val accessToken =
            refreshAccessToken(clientId, clientSecret, user.refreshToken, log = log).getString("access_token")
        val bookedSlot = BookedSlot(body, user)

        val response = addEvent(accessToken, user.email, apiKey, bookedSlot)
        if (response.has("status") && response.getString("status") == "confirmed")
            bookedSlotRepository.save(bookedSlot)
        return response.toString()

    }

    /**
     * TODO: /api/backend/cancel_event
     */

    /**
     * TODO: /api/backend/modify_event
     */

    /**
     * /api/backend/force_sync
     *
     * Force an update with Google Calendar's busy time periods for a given backend user.
     * @param email the backend user's email
     */
    @PostMapping("/api/backend/force_sync")
    fun force_sync(email: String): Any {
        ensureEnvExists(env)

        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")
        val apiKey = env.getProperty("api.provider.google.key")
        val user = getUser(email)
        val accessToken = refreshAccessToken(clientId!!, clientSecret!!, user.refreshToken)

        val busyList = getBusyTime(
            apiKey!!,
            accessToken.getString("access_token"),
            user.email,
            log=log
        )
        backendUserRepository.save(user.updateBusyTime(busyList))
        return successGenerator("Ok.", log=log)
    }

    /**
     * /api/backend/get_booked
     *
     * Return a list of booked time (booked through this server).
     *
     * @param email the calendar owner's email address.
     * @param limit (optional, default=100) the maximum amount of timeslots to return (max=100).
     * @param startDate (optional, default:today) the starting date (with format YYYY-mm-dd).
     * @return a JSON array of BookedSlot objects.
     */
    @GetMapping("/api/backend/get_booked")
    fun get_booked(email: String, limit: Int?=100, startDate: String?): Any{
        val start = if (startDate==null) LocalDate.now() else LocalDate.parse(startDate)
        val user = getUser(email)
        val bookedSlots: List<BookedSlot> = bookedSlotRepository.findAllByHost(user).sortedBy{ it.startTime }

        if (bookedSlots.isEmpty()) return listOf<Nothing>()
        var startIndex = -1
        for (i in bookedSlots.indices) {
            if (bookedSlots[i].startTime.toLocalDate()>=start) {
                startIndex = i
                break
            }
        }
        return bookedSlots.slice(startIndex until min(startIndex+limit!!,bookedSlots.size))
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

    fun ensureUserNonexist(email: String){
        val errMsg = "User already exists."
        if (backendUserRepository.findByEmail(email)!=null)
            throw Exception(errMsg)
    }

    fun ensureEnvExists(env: Environment?){
        val errMsg = "Environment is not injected."
        if (env==null)
            throw Exception(errMsg)
    }

}

