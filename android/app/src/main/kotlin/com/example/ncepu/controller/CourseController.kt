package com.example.ncepu.controller

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.example.ncepu.glance.TimetableWidget
import com.example.ncepu.glance.TimetableWidgetReceiver
import com.example.ncepu.model.Course
import com.example.ncepu.model.CourseJSON
import com.example.ncepu.model.CourseTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

class CourseController {
    val courseList = mutableListOf<Course>();
    val courseTimeList = mutableListOf<CourseTime>();

    fun getCurrentWeekID(): Int {
        return 5;
    }

    fun load(json: String?){
        if (json == null || json == "") {
            Log.w("CourseController", "timetable is null or empty");
            return;
        }
        val courseJSON: CourseJSON = Json.decodeFromString(json);
        courseList.addAll(courseJSON.courses);
        courseTimeList.addAll(courseJSON.courseTimes);
    }

    fun scheduleAlarm(context: Context, diffMinute: Int){
        if (diffMinute < 0) {
            return;
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            234,
            Intent(context, TimetableWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, diffMinute.toInt())

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmReceiver", "设置精确闹钟成功 ${diffMinute}")
        } else {
            Toast.makeText(context, "无法设置精确闹钟，请检查APP权限", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateWidget(context: Context?){
        if (context == null) return;
        val currentWeekID = getCurrentWeekID();
        val now = Calendar.getInstance();
        val topCourse = getCurrentCourse(currentWeekID, now);
        val nextCourse = getNextCourseOf(currentWeekID, now, topCourse);
        val topCourseStr = getWidgetDesc(currentWeekID, now, topCourse);
        val nextCourseStr = getWidgetDesc(currentWeekID, now, nextCourse);
        val mainShowEmpty = topCourseStr[0] == "";
        val hasNextClass = nextCourseStr[0] != "";
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean("mainShowEmpty", mainShowEmpty)
            .putBoolean("hasNextClass", hasNextClass)
            .putString("strDate", "${now.get(Calendar.MONTH) + 1}月${now.get(Calendar.DATE)}日")
            .putString("strWeek", "第 ${currentWeekID} 周")
            .putString("strTime", topCourseStr[0])
            .putString("strClassroom", topCourseStr[1])
            .putString("strCourse", topCourseStr[2])
            .putString("strDoW", getDoWStr(getDoW(now)))
            .putString("strCourseRemaining", getCourseRemainingStr(currentWeekID, now))
            .putString("strNextTime", nextCourseStr[0])
            .putString("strNextCourse", nextCourseStr[2])
            .putString("strNextClassroom", nextCourseStr[1])
            .putString("strEmpty", "无更多课程")
            .apply()
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("AlarmReceiver", "收到小组件更新广播")
            TimetableWidget().updateAll(context)
        }
    }

    fun export(): String {
        return Json.encodeToString(CourseJSON(courseList, courseTimeList));
    }

    fun getTime(span: Int) : CourseTime {
        if (span < 1 || span > courseTimeList.size) return courseTimeList[0];            // fallback
        return courseTimeList[span - 1];
    }

    // 获取当天全部课程
    // everything: 包括非本周课程
    fun getCourseOfDay(
        currentWeekID: Int,
        dow: Int,
        everything: Boolean = false) : List<Course> {
        val result = mutableListOf<Course>();
        for (course in courseList) {
            if (course.dow == dow) {
                if (course.weeks.contains(currentWeekID) || everything) {
                    result.add(course);
                }
            }
        }
        return result;
    }

    private fun getDoW(date: Calendar): Int {       // 转换为dart形式
        var dow = date.get(Calendar.DAY_OF_WEEK);
        if (dow == 1) dow = 8;
        return dow - 1;
    }

    // 计算“真时差”，包括天数不同的情况，此函数假设待计算课程在当天之后
    // 如果返回小于0，表示当前课程在当前时间 currentWeekID, now 之后不开课
    fun realDiff(
        currentWeekID: Int,
        now: Calendar,
        course: Course): Int {
        var deltaDay = course.dow + 7 - getDoW(now);
        var realWeekID = currentWeekID;
        if (deltaDay < 7) {
            realWeekID += 1;
        } else if (deltaDay == 7) {
            // 在同一天，但可能已经上完，判断是否借位
            val courseEnd = getTime(course.end);
            if (courseEnd.diff(now) < 0) {
                // 课程已经上完
                realWeekID += 1;
            } else {
                deltaDay = 0;
            }
        } else {
            // 目标课程在同一周，无需借位
            deltaDay -= 7;
        }
        // 起始周次补偿
        var deltaWeek = course.weeks[0] - realWeekID;
        if (deltaWeek < 0) deltaWeek = 0;

        if (course.weeks.last() < realWeekID) return -1;


        // 对于每一节，计算相对当前时间（同一天）或相对零点（另一天）的时间差
        val deltas = mutableListOf<Int>();
        if (deltaDay == 0 && deltaWeek == 0) {
            for (i in course.begin..course.end) {
                deltas.add(getTime(i).diff(now));
            }
        } else {
            for (i in course.begin..course.end) {
                val begin = getTime(i);
                deltas.add(
                    begin.startHour * 60
                            + begin.startMinute
                            - now.get(Calendar.HOUR_OF_DAY) * 60
                            - now.get(Calendar.MINUTE));
            }
        }
        val deltaWholeDay = (deltaDay + deltaWeek * 7) * 60 * 24;

        // 特殊情况：如果正在上课，直接返回0
        if (deltaWholeDay == 0 && deltas.contains(0)) return 0;

        // 找出时差最短的作为结果
        var deltaMin = Int.MAX_VALUE;
        var isBetweenClass = false;
        for (delta in deltas) {
            if (delta < 0 && isBetweenClass == true) return 0;      // 特殊情况：课间时课程数据更新，delta中将同时包含正负
            if (delta >= 0) isBetweenClass = true;
            deltaMin = deltaMin.coerceAtMost(delta);
        }

        val result = deltaMin + deltaWholeDay;
        return result;
    }

    // 获取当前时间对应的课程，如果为大课间，则获取下一节课（可以是未来任一天）
    fun getCurrentCourse(
        currentWeekID: Int,
        now: Calendar): Course? {
        var result: Course? = null;
        var deltaMin = Int.MAX_VALUE;
        for (course in courseList) {
            val delta = realDiff(currentWeekID, now, course);
            if (delta < 0) continue;
            if (delta < deltaMin) {
                deltaMin = delta;
                result = course;
            }
        }
        return result;
    }

    // 获取某一节课的下一节课（传入null表示获取当前课程的下一节课）
    fun getNextCourseOf(
        currentWeekID: Int,
        now: Calendar,
        base: Course?
    ): Course? {
        var deltaBase = 0;
        if (base != null) deltaBase = realDiff(currentWeekID, now, base);
        if (deltaBase < 0) deltaBase = 0;       // base已经结束，获取下一节
        var result: Course? = null;
        var deltaMin = Int.MAX_VALUE;
        for (course in courseList) {
            val delta = realDiff(currentWeekID, now, course);
            if (delta <= deltaBase) continue;
            if (delta < deltaMin) {
                deltaMin = delta;
                result = course;
            }
        }
        return result;
    }

    // 获取当天还剩余的课程数量
    fun getCourseRemaining(
        currentWeekID: Int,
        now: Calendar): Int {
        var remaining = 0;
        val courseOfToday = getCourseOfDay(currentWeekID, getDoW(now));
        for (course in courseOfToday) {
            val startTime = getTime(course.begin);
            val delta = startTime.diff(now);
            if (delta <= 0 || delta > 24 * 60) continue;
            remaining += 1;
        }
        return remaining;
    }

    // UI相关字符串生成
    fun getDoWStr(dow: Int): String {
        if (dow < 1 || dow > 7) return "";
        val weeksChinese = listOf("一", "二", "三", "四", "五", "六", "日");
        return "周" + weeksChinese[dow - 1];
    }

    // 获取目标日期在当天对应字符串
    fun getDayStr(
        now: Calendar,
        target: Calendar): String {
        val nowDate = (now.time.time + 3600 * 8 * 1000) / 86400000;
        val targetDate = (target.time.time + 3600 * 8 * 1000) / 86400000;
        val deltaDate = targetDate - nowDate;
        if (deltaDate == 0L) {
            return "今天";
        } else if (deltaDate == 1L) {
            return "明天";
        } else if (deltaDate == 2L) {
            return "后天";
        } else if (deltaDate > (14 - getDoW(now))) {
            // 超过2周，显示实际日期
            return "${now.get(Calendar.MONTH) + 1}/${now.get(Calendar.DATE)}";
        }
        val next = if (deltaDate > 7 - getDoW(now)) "下" else "";
        return next + getDoWStr(getDoW(target));
    }

    // 获取小组件上/下半部分的描述
    fun getWidgetDesc(
        currentWeekID: Int,
        now: Calendar,
        course: Course?
    ): List<String> {
        var strTime = "";
        if (course == null) return listOf("", "", "");
        val diff = realDiff(currentWeekID, now, course);
        if (diff < 0) {
            strTime = "课程已结束";
        } else {
            val courseDate = Calendar.getInstance()
            courseDate.add(Calendar.MINUTE, diff);
            val courseTime = getTime(course.begin);
            strTime = getDayStr(
                now,
                courseDate
            ) + " ${courseTime.startHour.toString().padStart(2, '0')}:${courseTime.startMinute.toString().padStart(2, '0')}";
        }
        return listOf(
            strTime,
            course.classroom,
            course.name,
        );
    }

    fun getCourseRemainingStr(
        currentWeekID: Int,
        now: Calendar
    ): String {
        val remaining = getCourseRemaining(currentWeekID, now);
        if (remaining == 0) {
            return "今日课程已上完";
        } else if (remaining == 1) {
            return "今天最后一节";
        }
        return "今天还有${remaining}节";
    }

    fun getNextRefreshMinute(
        currentWeekID: Int,
        now: Calendar
    ): Int {
        val next = getCurrentCourse(currentWeekID, now) ?: return 60;
        val real = realDiff(currentWeekID, now, next);
        if (real < 0 || real > 60) return 60;
        val deltas = mutableListOf<Int>();
        for (i in next.begin..next.end) {
            deltas.add(getTime(i).diff(now));
        }
        var minDelta = 60;
        var changed = false;
        for (delta in deltas) {
            if (delta <= 0) continue;
            changed = true;
            minDelta = minDelta.coerceAtMost(delta);
        }
        if (!changed) {
            // 最后一个小节，需要计算现在到这小节下课的时间差
            val calendar = Calendar.getInstance();
            val end = getTime(next.end);
            calendar.set(Calendar.HOUR_OF_DAY, end.endHour);
            calendar.set(Calendar.MINUTE, end.endMinute);
            var delta = calendar.timeInMillis - now.timeInMillis;
            delta /= 60 * 1000;
            if (delta > 0) return delta.toInt();
        }
        return minDelta;
    }
}
