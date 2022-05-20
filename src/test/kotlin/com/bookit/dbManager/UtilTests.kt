package com.bookit.dbManager

import com.bookit.dbManager.db.AvailableTime
import com.bookit.dbManager.util.mergeAvailableTime
import com.bookit.dbManager.util.mergeIntervals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.OffsetDateTime

@SpringBootTest
class utilTests {
    fun <T: Comparable<T>> checkEqual(a: List<Pair<T,T>>, b: List<Pair<T,T>>): Boolean{
        if (a.size!=b.size) return false
        return a.indices.all { a[it].first==b[it].first && a[it].second==b[it].second }
    }
    @Test
    fun mergeIntervalsTest() {
        val t = listOf(
            listOf(Pair(10,20), Pair(30,40), Pair(50,60)),
            listOf(Pair(10,30), Pair(30,40), Pair(50,60)),
            listOf(Pair(30,50), Pair(10,35), Pair(60,70)),
            listOf(Pair(60,70), Pair(10,45), Pair(20,30)))
        val tt = listOf(
                Pair(OffsetDateTime.parse("2019-08-31T15:20:30+08:00"), OffsetDateTime.parse("2019-08-31T16:23:30+08:00")),
                Pair(OffsetDateTime.parse("2019-08-30T11:20:30+08:00"), OffsetDateTime.parse("2019-08-30T16:27:30+08:00")),
                Pair(OffsetDateTime.parse("2019-08-31T15:50:30+08:00"), OffsetDateTime.parse("2019-08-31T20:21:30+05:00")),
                Pair(OffsetDateTime.parse("2019-08-31T23:20:30+08:00"), OffsetDateTime.parse("2019-08-31T23:28:30+08:00"))
            )

        val a = listOf(
            listOf(Pair(10,20), Pair(30,40), Pair(50,60)),
            listOf(Pair(10,40), Pair(50,60)),
            listOf(Pair(10,50), Pair(60,70)),
            listOf(Pair(10,45), Pair(60,70))
        )
        val at = listOf(
            Pair(OffsetDateTime.parse("2019-08-30T11:20:30+08:00"), OffsetDateTime.parse("2019-08-30T16:27:30+08:00")),
            Pair(OffsetDateTime.parse("2019-08-31T15:20:30+08:00"), OffsetDateTime.parse("2019-08-31T23:28:30+08:00")),
        )

        for (i in t.indices) {
            //println(mergeIntervals(t[i]).map { listOf(it.first, it.second) })
            assert(checkEqual(mergeIntervals(t[i]), a[i]))
        }
        //println(mergeIntervals(tt))
        assert(checkEqual(mergeIntervals(tt), at))
    }

    @Test
    fun mergeAvailableTimeTest(){
        val t = listOf(
            listOf(Pair(10,20), Pair(30,40), Pair(50,60)),
            listOf(Pair(10,30), Pair(30,40), Pair(50,60)),
            listOf(Pair(30,50), Pair(10,35), Pair(60,70)),
            listOf(Pair(60,70), Pair(10,45), Pair(20,30))
        ).map { it.map { AvailableTime(it.first, it.second) } }
        val a = listOf(
            listOf(Pair(10,20), Pair(30,40), Pair(50,60)),
            listOf(Pair(10,40), Pair(50,60)),
            listOf(Pair(10,50), Pair(60,70)),
            listOf(Pair(10,45), Pair(60,70))
        ).map { it.map { AvailableTime(it.first, it.second) } }

        for (i in t.indices) {
            assert(checkEqual(
                mergeAvailableTime(t[i]).map { Pair(it.startMinute, it.endMinute) },
                a[i].map { Pair(it.startMinute, it.endMinute) }))
        }
    }

}


