package com.bookit.dbManager.api

import com.bookit.dbManager.api.src.getHistory
import com.bookit.dbManager.db.*
import com.bookit.dbManager.exceptions.*
import com.bookit.dbManager.util.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.annotation.Resource

@SpringBootApplication
@RestController
class SecuredAPIController @Autowired constructor(
    val historyRepository: HistoryRepository,
    val backendUserRepository: BackendUserRepository,
    val scheduleTypeRepository: ScheduleTypeRepository,
) {
    val log = logger<SecuredAPIController>()

    @Resource
    private val env: Environment? = null

    @Operation(summary = "Send a welcome message if authorization is passed.")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/welcome", consumes = [], produces = [])
    fun welcome(): Any {
        return successResponse("Welcome. The authorization token is correct.")
    }

    @Operation(summary = "Update the refresh token.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/update_refresh_token", consumes = ["application/json"], produces = [])
    fun update_refresh_token(
        @RequestBody body: UpdateRefreshToken
    ): ResponseEntity<String> {
        ensureEnvExists(env)

        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")

        val user: BackendUser = getUser(body.email)

        if (checkRefreshToken(clientId, clientSecret, body.refreshToken, log = log)) {
            val updatedUser = user.updateRefreshToken(body.refreshToken)
            backendUserRepository.save(updatedUser)
        } else
            throw InvalidRefreshTokenException()

        return successResponse("Ok.", log = log)
    }

    @Operation(summary = "Add a backend user.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/add_user", consumes = ["application/json"], produces = [])
    fun add_user(@RequestBody body: AddBackendUser): Any {
        ensureUserNonexist(body.email)
        backendUserRepository.save(BackendUser(body))
        return successResponse("Ok.", log = log)
    }

    @Operation(summary = "Add a schedule type.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/add_schedule_type", consumes = ["application/json"], produces = [])
    fun add_schedule_type(@RequestBody body: AddScheduleType): Any {
        val user: BackendUser = getUser(body.email)
        if (!validDayOfWeekEncoding(body.availableDays)) throw InvalidValueException()
        scheduleTypeRepository.save(ScheduleType(body, user))
        return successResponse("Ok.", log = log)
    }

    @Operation(summary = "Change the available time of a schedule type.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/{token}/change_available_time", consumes = ["application/json"], produces = [])
    fun change_available_time(
        @Parameter(description = "the token of the schedule type.") @PathVariable token: String,
        @RequestBody body: List<AddAvailableTime>
    ): Any {
        val scheduleType = getScheduleType(token)
        scheduleTypeRepository.save(scheduleType.updateAvailableTime(body.map { it.toAvailableTime() }))
        return successResponse("Ok.", log = log)
    }

    /**
     * TODO: design /api/backend/cancel_event
     */

    /**
     * TODO: design /api/backend/modify_event
     */

    @Operation(
        summary = "Force a sync with Google calendar.",
        description = "Sync the database record of the busy time of the given backend user with their Google calendar"
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @PostMapping("/api/backend/force_sync", consumes = [], produces = [])
    fun force_sync(@Parameter(description = "the email of the backend user.") email: String): Any {
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
            log = log
        )
        backendUserRepository.save(user.updateBusyTime(busyList))
        return successResponse("Ok.", log = log)
    }

    @Operation(summary = "Get backend user details.", description = "")
    //@ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @GetMapping("/api/backend/get_user", consumes = ["application/json"], produces = ["application/json"])
    fun get_user(@Parameter(description = "the email of the backend user.") email: String): BackendUser {
        val user = getUser(email)
        return user
    }

    @Operation(summary = "Get schedule type details.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @GetMapping("/api/backend/{token}", consumes = [], produces = ["application/json"])
    fun get_details(@Parameter(description = "the email of the backend user.") @PathVariable token: String): ScheduleType {
        val scheduleType = getScheduleType(token)
        return scheduleType
    }

    @Operation(summary = "Delete schedule type.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))
    @DeleteMapping("/api/backend/{token}", consumes = [], produces = [])
    fun delete_schedule_type(@Parameter(description = "the email of the backend user.") @PathVariable token: String): ResponseEntity<String> {
        val scheduleType = getScheduleType(token)
        scheduleTypeRepository.delete(scheduleType)
        return successResponse("Ok.", log = log)
    }


    @Operation(summary = "Get the booking history for a backend user.", description = "")
    @ApiResponses(ApiResponse(responseCode = "200", description = "successful operation"))

    @GetMapping("/api/backend/get_history", consumes = ["application/json"], produces = ["application/json"])
    fun get_history(@RequestBody body: GetHistoryRequest): List<BookTimeSlot> {
        val pageLimit = if (body.limit <= 100) body.limit else 100
        val start = if (body.startDate == null) LocalDate.now() else LocalDate.parse(body.startDate)
        val user = getUser(body.email)
        val bookingHistories: List<Booking> = historyRepository.findAllByHost(user)

        return getHistory(pageLimit, start, bookingHistories)
    }


    fun getScheduleType(scheduleTypeToken: String): ScheduleType {
        return scheduleTypeRepository.findByToken(scheduleTypeToken)
            ?: throw ScheduleTypeNotFoundException()
    }

    fun getUser(email: String): BackendUser {
        return backendUserRepository.findByEmail(email)
            ?: throw UserNotFoundException()
    }

    fun ensureUserNonexist(email: String) {
        if (backendUserRepository.findByEmail(email) != null)
            throw UserAlreadyExistException()
    }

}

