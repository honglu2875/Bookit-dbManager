package com.bookit.dbManager.api

import kotlinx.serialization.Serializable

// Shadows of database entities used for api inputs

@Serializable
data class AddBookedSlot(
    val hostEmail: String,
    val startTime: String,
    val endTime: String,
    val attendees: List<AddAttendee>,
    val description: String = ""
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
)

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