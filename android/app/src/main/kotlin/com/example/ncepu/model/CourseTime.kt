package com.example.ncepu.model

import kotlinx.serialization.Serializable
import java.util.Calendar
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Serializable
data class CourseTime(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
) {
    private val minuteStart = startHour * 60 + startMinute
    private val minuteEnd = endHour * 60 + endMinute

    fun diff(now: Calendar): Int {
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        if (hour == endHour && minute == endMinute) return -1;
        val minuteTotal = hour * 60 + minute
        if (minuteTotal < minuteStart) return minuteStart - minuteTotal
        if (minuteTotal > minuteEnd) return - (minuteTotal - minuteEnd)
        return 0;
    }
}
