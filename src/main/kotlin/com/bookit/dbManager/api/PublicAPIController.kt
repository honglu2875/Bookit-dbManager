package com.bookit.dbManager.api

import com.bookit.dbManager.api.src.getTimeslot
import com.bookit.dbManager.db.*
import com.bookit.dbManager.exceptions.ScheduleTypeNotFoundException
import com.bookit.dbManager.exceptions.UserNotFoundException
import com.bookit.dbManager.util.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.annotation.Resource

@SpringBootApplication
@RestController
class PublicAPIController @Autowired constructor(
    val historyRepository: HistoryRepository,
    val backendUserRepository: BackendUserRepository,
    val scheduleTypeRepository: ScheduleTypeRepository,
    val lockedSlotRepository: LockedSlotRepository
) {
    val log = logger<PublicAPIController>()

    @Resource
    private val env: Environment? = null

    @Operation(summary = "Return a welcome string.")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @GetMapping("/api")
    fun api(model: Model): ResponseEntity<String> {
        return successResponse("Welcome. This is a public endpoint.")
    }

    @Operation(
        summary = "Get available time slots.", description = """
        A public api to obtain a list of available slots according to the schedule type and the calendar availability.
        A timeslot is generated sequentially in each available time period described in ScheduleType.availableTime.
        It goes as far as 3 months from the starting date for every single request.
        The api only uses the latest synced data without sending queries to Google Calendar. If the fresh data is required, please call /api/backend/force_sync beforehand.
     """
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @GetMapping("/api/get_timeslot", consumes = [], produces = ["application/json"])
    fun get_timeslot(
        email: String,
        token: String,
        limit: Int?,
        startDate: String?
    ): ResponseEntity<List<Pair<OffsetDateTime, OffsetDateTime>>> {
        val user = getUser(email)
        val scheduleType = getScheduleType(token)
        val lockedSlots: List<LockedSlot> = lockedSlotRepository.findAllByHost(user)
        val startLocalDate = if (startDate == null) LocalDate.now() else LocalDate.parse(startDate)
        val pageLimit = limit ?: 100

        return successResponse(getTimeslot(pageLimit, startLocalDate, user, scheduleType, lockedSlots), log = log)
    }

    @Operation(
        summary = "Add events.", description = """
        Add a new event to a backend user's default calendar (calendarId=email). If the response from Google contains "status" key and the value is "confirmed", a history record of the booked event will be persisted in the database.
     """
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/add_event", consumes = ["application/json"], produces = ["application/json"])
    fun add_event(@RequestBody body: BookTimeSlot): Any {
        val user: BackendUser = getUser(body.hostEmail)
        ensureEnvExists(env)

        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")!!
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")!!
        val apiKey = env.getProperty("api.provider.google.key")!!
        val accessToken =
            refreshAccessToken(clientId, clientSecret, user.refreshToken, log = log).getString("access_token")
        val booking = Booking(body, user)

        val response = addEvent(accessToken, user.email, apiKey, booking)

        if (response.has("status") && response.getString("status") == "confirmed") {
            historyRepository.save(booking)
            return successResponse("Booking confirmed.", log = log)
        } else
            return response.toString()
    }

    @Operation(
        summary = "Lock up a time slot.", description = """
        Temporarily lock up a timeslot for a backend user.
    """
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/lock_slot", consumes = ["application/json"], produces = ["application/json"])
    fun lock_slot(@RequestBody body: AddLockedSlot): ResponseEntity<LockedSlot> {
        val user = getUser(body.hostEmail)
        val lockedSlot = LockedSlot(body, user)
        lockedSlotRepository.save(lockedSlot)
        return successResponse(lockedSlot, log = log)
    }


    fun getScheduleType(scheduleTypeToken: String): ScheduleType {
        return scheduleTypeRepository.findByToken(scheduleTypeToken)
            ?: throw ScheduleTypeNotFoundException("Schedule type not found.")
    }

    fun getUser(email: String): BackendUser {
        return backendUserRepository.findByEmail(email)
            ?: throw UserNotFoundException("User not found.")
    }
}

