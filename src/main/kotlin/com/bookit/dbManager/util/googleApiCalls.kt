package com.bookit.dbManager.util

import com.bookit.dbManager.db.Booking
import com.bookit.dbManager.db.BusyTime
import org.json.JSONObject
import org.slf4j.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.OffsetDateTime

const val googleApiURI = "https://oauth2.googleapis.com/token"
const val googleApiEndpoint = "https://www.googleapis.com/calendar/v3/calendars"
const val googleFreeBusyEndpoint = "https://www.googleapis.com/calendar/v3/freeBusy"
const val createEventURIPreamble = "conferenceDataVersion=1&sendNotifications=true"


/**
 * Check whether the refresh token is valid (does not double-check the scope).
 *
 * The function will try to check whether a refresh token is valid by requesting a new access token.
 *
 * @param clientId the client ID.
 * @param clientSecret the client secret.
 * @param refreshToken the refresh token string.
 * @param log the logger.
 * @return true or false depending on whether the refresh token is valid.
 */
fun checkRefreshToken(
    clientId: String?,
    clientSecret: String?,
    refreshToken: String,
    log: Logger?=null
    ): Boolean {
    if (clientId==null || clientSecret==null) {
        log?.debug("checkRefreshToken: client ID or client secret is null.")
        return false
    }
    val responseObj = refreshAccessToken(clientId, clientSecret, refreshToken, log)
    return responseObj.has("access_token")
}


/**
 * Refresh the access token.
 *
 * Use the refresh token to request a new access token. See RFC 4769 section 6.
 * (client_id and client_secret are put into the request body. They being in header is ***NOT YET IMPLEMENTED***.)
 *
 * @param clientId the client ID.
 * @param clientSecret the client secret.
 * @param refreshToken the refresh token string.
 * @param log the logger.
 * @return a map between Strings representing the response from server, plus an additional "status" key.
 */
fun refreshAccessToken(
    clientId: String,
    clientSecret: String,
    refreshToken: String,
    log: Logger?=null
): JSONObject {

    val params = mapOf(
        "client_id" to clientId,
        "client_secret" to clientSecret,
        "refresh_token" to refreshToken,
        "grant_type" to "refresh_token"
    )
    val requestBody: String = JSONObject(params).toString()
    log?.debug("Request:${requestBody}")

    val httpclient = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(googleApiURI))
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build()

    val response = httpclient.send(request, HttpResponse.BodyHandlers.ofString())
    val responseObj = JSONObject(response.body())
    log?.debug("$responseObj")
    return responseObj
}


/**
 * Get busy time periods.
 *
 * Get busy time periods starting from the current time.
 *
 * @param apiKey the api key for Google Calendar API.
 * @param accessToken the access token.
 * @param email the email.
 * @param monthsAhead the number of months to look up. Default to 3 months (the max number of months for Google API).
 * @param log the logger.
 * @return a list of BusyTime objects indicating the busy time periods of the backend user with the given email.
 */
fun getBusyTime(
    apiKey: String,
    accessToken: String,
    email: String,
    monthsAhead: Int=3,
    log: Logger? = null
): List<BusyTime> {
    val params = mapOf(
        "timeMin" to OffsetDateTime.now().toISO(),
        "timeMax" to OffsetDateTime.now().plusMonths(monthsAhead.toLong()).toISO(),
        "items" to listOf(
            // The main calendar of an account has the email address as an identifier. We do not expand the functionality to other calendars for now.
            mapOf(
                "id" to email
            )
        )
    )
    val requestBody: String = JSONObject(params).toString()
    log?.debug("Request:${requestBody}")

    val httpclient = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("${googleFreeBusyEndpoint}?key=${apiKey}"))
        .header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build()

    val response = httpclient.send(request, HttpResponse.BodyHandlers.ofString())
    val responseObj = JSONObject(response.body())
    log?.debug("Response:$responseObj")

    // Parse the response
    val result = arrayListOf<BusyTime>()
    val list = responseObj.getJSONObject("calendars").getJSONObject(email).getJSONArray("busy")
    for (ind in 0 until list.length()) {
        val item = list.getJSONObject(ind)
        result.add(
            BusyTime(
                OffsetDateTime.parse(item.getString("start")),
                OffsetDateTime.parse(item.getString("end"))
            )
        )
    }
    return result.toList()
}


/**
 * Add a calendar event if there is no overlap
 *
 * Use the access token and the api key to add a calendar event, if it does not overlap with already-booked events.
 *
 * @param accessToken the access token string.
 * @param calendarID the calendar ID in the Google account (the default calendar: ID == email address).
 * @param apikey the Google Calendar api key.
 * @param event a BookedSlot object.
 * @param log a logger.
 * @param withMeetUrl true (default) if to generate a Google Meet Url.
 * @return the response as a JSONObject.
 */
fun addEvent(
    accessToken: String,
    calendarID: String,
    apikey: String,
    event: Booking,
    log: Logger? = null,
    withMeetUrl: Boolean = true
): JSONObject {

    if (event.startTime.isBefore(OffsetDateTime.now())) throw Exception("Can't book an earlier date.")
    val conferenceDataJSON = mapOf("createRequest" to mapOf(
        "conferenceSolutionKey" to mapOf("type" to "hangoutsMeet"),
        "requestId" to randomMeetString())
    )

    // TODO: PUT IN EVENT DETAILS
    val params = mapOf(
        "start" to mapOf("dateTime" to event.startTime.toISO()),
        "end" to mapOf("dateTime" to event.endTime.toISO()),
        "conferenceData" to if (withMeetUrl) conferenceDataJSON else null,
        "attendees" to event.attendees.map {
            mapOf(
                "email" to it.attendeeEmail,
                "displayName" to it.attendeeName
            )
        }
    )

    val uri = "${googleApiEndpoint}/${calendarID}/events?${createEventURIPreamble}&key=${apikey}"
    val httpclient = HttpClient.newBuilder().build()
    val requestBody: String = JSONObject(params).toString()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(uri))
        .header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build()
    val response = httpclient.send(request, HttpResponse.BodyHandlers.ofString())
    val responseObj = JSONObject(response.body())

    log?.debug(responseObj.toString())
    return responseObj
}

