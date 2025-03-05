package com.example.ncepu

import android.content.Context
import android.widget.Toast
import com.example.ncepu.controller.CourseController
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.Calendar

class MainActivity : FlutterActivity(){
    private val CHANNEL_NAME = "channel.ncepu.example.com"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME).setMethodCallHandler {
                call, result ->
            if (call.method == "alarm") {
                updateAlarm(call.arguments<String>());
            }
        }
    }

    private fun updateAlarm(json: String?) {
        var jsonLocal = json;
        val controller = CourseController();
        if (jsonLocal == null || jsonLocal == "") {
            val preferences = context.getSharedPreferences("timetable", Context.MODE_PRIVATE);
            jsonLocal = preferences.getString("timetable", null);
        }
        controller.load(jsonLocal);
        controller.updateWidget(context);
        controller.scheduleAlarm(
            this,
            controller.getNextRefreshMinute(
                controller.getCurrentWeekID(),
                Calendar.getInstance()
            ));
        if (json != null && json != "") {
            val preferences = context.getSharedPreferences("timetable", Context.MODE_PRIVATE);
            preferences.edit()
                .putString("timetable", json)
                .apply();
            Toast.makeText(this, "Kotlin存储更新", Toast.LENGTH_SHORT).show()
        }
    }
}