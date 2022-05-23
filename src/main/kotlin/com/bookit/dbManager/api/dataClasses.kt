package com.bookit.dbManager.api

import com.bookit.dbManager.db.AvailableTime
import kotlinx.serialization.Serializable

// Shadows of database entities used for api inputs

@Serializable
data class BookTimeSlot(
    val hostEmail: String,
    val startTime: String,
    val endTime: String,
    val attendees: List<AddAttendee>,
    val description: String = ""
)

@Serializable
data class AddLockedSlot(
    val hostEmail: String,
    val startTime: String,
    val endTime: String,
    val expiration: Int = 10
)

@Serializable
data class AddAttendee(
    val email: String,
    val name: String = ""
)

@Serializable
data class AddScheduleType(
    val email: String,
    val duration: Int,
    val description: String = "",
    val zoneId: String = "Z",
    val availableList: List<AddAvailableTime> = listOf(AddAvailableTime(480, 1020)),
    val availableDays: Int = "1111100".toInt(2),
    val timeBetweenSlots: Int = 0
)

@Serializable
data class AddAvailableTime(
    val startMinute: Int,
    val endMinute: Int
) {
    fun toAvailableTime() = AvailableTime(startMinute, endMinute)
}

@Serializable
data class AddBackendUser(
    val email: String,
    val refreshToken: String,
    val name: String? = null,
)

@Serializable
data class UpdateRefreshToken(
    val email: String,
    val refreshToken: String
)

@Serializable
data class GetHistoryRequest(
    val email: String,
    val limit: Int = 100,
    val startDate: String? = null
)