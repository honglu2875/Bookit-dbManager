package com.bookit.dbManager.db

import com.bookit.dbManager.api.AddBackendUser
import com.bookit.dbManager.api.AddBookedSlot
import com.bookit.dbManager.api.AddScheduleType
import com.bookit.dbManager.util.getToken
import com.bookit.dbManager.util.mergeAvailableTime
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.persistence.*

/**
 * a backend user
 *
 * A backend user who intends to be booked by others in public based on Google calendar availability.
 *
 * @property email the email address (uniquely identifiable).
 * @property refreshToken the refresh token of the user's Google API.
 * @property name (optional) the name of the user.
 * @property createdAt (optional) the timestamp of creation time (automatically generated).
 * @property lastUpdated (optional) the time of the most recent update of busyPeriods.
 * @property busyPeriods (optional) list of BusyTime classes documenting busy timeslots.
 */
@Entity
class BackendUser (
    @Id val email: String,
    val refreshToken: String,
    val name: String? = null,
    @Column(columnDefinition = "timestamp with time zone")
    private val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(columnDefinition = "timestamp with time zone")
    private val lastUpdated: OffsetDateTime? = null,
    @ElementCollection @CollectionTable(name="busyTime")
    val busyPeriods: List<BusyTime> = listOf()
    )
{
    constructor(addBackendUser: AddBackendUser): this(
            email = addBackendUser.email,
            refreshToken = addBackendUser.refreshToken,
            name = addBackendUser.name,
        )

    fun updateRefreshToken(newRefreshToken: String) = BackendUser(
        email,
        newRefreshToken,
        name,
        createdAt,
        lastUpdated,
        busyPeriods
    )

    fun updateBusyTime(newBusyList: List<BusyTime>) = BackendUser(
        email,
        refreshToken,
        name,
        createdAt,
        lastUpdated,
        newBusyList
    )

}



/**
 * a busy timeslot
 *
 * @property startTime the start time of the slot (OffsetDateTime; can be represented by, e.g., ISO8601)
 * @property endTime the end time of the slot (OffsetDateTime; can be represented by, e.g., ISO8601)
 */
@Embeddable
class BusyTime (
    @Column(columnDefinition = "timestamp with time zone")
    val startTime: OffsetDateTime,
    @Column(columnDefinition = "timestamp with time zone")
    val endTime: OffsetDateTime
    )

/**
 * a timeslot that has been booked
 *
 * @property host the host of the meeting (BackendUser class)
 * @property startTime the start time of the slot (OffsetDateTime; can be represented by, e.g., ISO8601)
 * @property endTime the end time of the slot (OffsetDateTime; can be represented by, e.g., ISO8601)
 * @property attendee (optional) list of attendees
 * @property description (optional) description text
 * @property id (optional) an automatically generated id number
 */
@Entity
class BookedSlot (
    @ManyToOne val host: BackendUser,
    @Column(columnDefinition = "timestamp with timezone")
    val startTime: OffsetDateTime,
    @Column(columnDefinition = "timestamp with timezone")
    val endTime: OffsetDateTime,
    @ElementCollection @CollectionTable(name="attendee")
    val attendee: List<Attendee> = listOf(),
    private val description: String? = null,
    @Id @GeneratedValue val id: Long? = null
    ){
    constructor(addBookedSlot: AddBookedSlot, user: BackendUser): this(
            host = user,
            startTime = OffsetDateTime.parse(addBookedSlot.startTime),
            endTime = OffsetDateTime.parse(addBookedSlot.endTime),
            attendee = addBookedSlot.attendees.map { Attendee(
                attendeeEmail = it.email,
                attendeeName = it.name
            ) },
            description = addBookedSlot.description
        )

}

/**
 * describe an attendee of a meeting.
 *
 * @property attendeeEmail the email of the attendee
 * @property attendeeName the name of the attendee. Can be null (anonymous).
 */
@Embeddable
class Attendee (
    val attendeeEmail: String,
    val attendeeName: String = ""
    )

/**
 * a schedule type recording the duration of each session and the available time for booking.
 *
 * @property host the host of the meetings (BackendUser).
 * @property duration (optional, default=60) duration of each booking slot (in minutes).
 * @property description (optional) description text.
 * @property zoneId (optional, default:UTC) the timezone.
 * @property availableList (optional, default: 8am-5pm) a list of AvailableTime object describing available time of the day (in the host's timezone). Time is represented by number of minutes after 12am.
 * @property timeBetweenSlots (optional, default=0) number of minutes between two booking slots.
 * @property availableDays (optional, default=0x7C) an integer encoding M/T/W/Th/F/Sa/Su availability in terms of binary bits. 1 for available and 0 for unavailable. E.g., 1111100 -> Mon-Fri available.
 * @property id (optional) an automatically generated id number.
 * @property token (optional) a 6-character unique token for this schedule type. In case of hash conflict, insertion will fail.
 */
@Entity
class ScheduleType constructor(
    @ManyToOne val host: BackendUser,
    val duration: Int = 60,
    val description: String = "",
    val zoneId: ZoneId = ZoneId.of("Z"),
    @ElementCollection @CollectionTable(name="available_time")
    val availableList: List<AvailableTime> = listOf(AvailableTime(480, 1020)), // default availability: 8am-5pm
    val timeBetweenSlots: Int = 0,
    val availableDays: Int = "1111100".toInt(2), // encode MTWThFSaSu availability as a binary int
    @Id @GeneratedValue
    val id: Long? = null,
    @Column(unique=true)
    var token: String = getToken(System.currentTimeMillis())
){


    constructor(addScheduleType: AddScheduleType, user: BackendUser): this(
            host=user,
            duration=addScheduleType.duration,
            description=addScheduleType.description,
            zoneId=ZoneId.of(addScheduleType.zoneId),
            availableList= mergeAvailableTime(
                addScheduleType.availableList.map { AvailableTime(it.startMinute, it.endMinute) }),
            availableDays=addScheduleType.availableDays,
            timeBetweenSlots=addScheduleType.timeBetweenSlots
        )


    fun updateAvailableTime(availableList: List<AvailableTime>) = ScheduleType(
        host,
        duration,
        description,
        zoneId,
        availableList = mergeAvailableTime(availableList),
        timeBetweenSlots,
        availableDays,
        id,
        token
    )
}


/**
 * a timeslot during the day when bookings can be made (relative to local timezone). No timeslot over midnight.
 *
 * @property startMinute start time (in minutes after 12am).
 * @property endMinute end time (in minutes after 12am).
 */
@Embeddable
class AvailableTime (
    val startMinute: Int,
    val endMinute: Int
    )

/**
 * a server-to-server API tokens.
 *
 * @property scope (optional) scope string, separated by "," (CURRENTLY NOT IN USE).
 * @property token (optional) the randomized token string
 * @property id (optional) an automatically generated id number
 */
@Entity
class ApiToken (
    var scope: String = "",
    @Column(unique=true)
    var token: String = getToken(System.currentTimeMillis(), lenOfToken=16),
    @Id @GeneratedValue
    val id: Long? = null
)