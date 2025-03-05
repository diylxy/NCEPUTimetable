package com.example.ncepu.glance

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.ncepu.controller.CourseController
import java.util.Calendar

class TimetableWidgetReceiver: GlanceAppWidgetReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences("timetable", Context.MODE_PRIVATE);
        val json = preferences.getString("timetable", null);
        val controller = CourseController();
        controller.load(json);
        controller.updateWidget(context);
        controller.scheduleAlarm(context, controller.getNextRefreshMinute(
            controller.getCurrentWeekID(),
            Calendar.getInstance(),
        ));
        super.onReceive(context, intent)
    }
    override val glanceAppWidget: GlanceAppWidget
        get() = TimetableWidget()
}