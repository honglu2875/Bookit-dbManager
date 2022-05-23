package com.bookit.dbManager.api.src

import com.bookit.dbManager.api.BookTimeSlot
import com.bookit.dbManager.db.Booking
import java.time.LocalDate
import kotlin.math.min

fun getHistory(pageLimit: Int, start: LocalDate, bookingHistories: List<Booking>): List<BookTimeSlot> {
    if (bookingHistories.isEmpty()) return listOf()
    var startIndex = -1
    for (i in bookingHistories.indices) {
        if (bookingHistories[i].startTime.toLocalDate() >= start) {
            startIndex = i
            break
        }
    }
    return bookingHistories
        .sortedBy { it.startTime }
        .slice(startIndex until min(startIndex + pageLimit, bookingHistories.size))
        .map { it.toBookTimeSlot() }
}