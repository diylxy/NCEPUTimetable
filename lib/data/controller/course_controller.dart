import 'dart:convert';
import 'dart:math';

import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:ncepu/data/model/course.dart';
import 'package:shared_preferences/shared_preferences.dart';

class CourseController extends GetxController {
  static CourseController get to => Get.find();
  final List<Course> courseList = []; // 课程列表
  final List<CourseTime> courseTimeList = []; // 课程时间列表，注意下标
  DateTime selectedDate = DateTime.now();   // 当前用户选择的时间

  void updateSelectedDate(DateTime date) {
    selectedDate = date;
    update();
  }

  String exportJSON() {
    final exports = {
      "courses": List.from(courseList.map((e) => e.toMap())),
      "courseTimes": List.from(courseTimeList.map((e) => e.toMap())),
    };
    return jsonEncode(exports);
  }

  void importJSON(String? json) {
    if (json == null) return;
    courseList.clear();
    courseTimeList.clear();
    try {
      final imports = jsonDecode(json) as Map<String, dynamic>;
      final courses = imports["courses"] as List<dynamic>;
      final courseTimes = imports["courseTimes"] as List<dynamic>;
      for (Map<String, dynamic> course in courses) {
        final weeks = List.of(
          (course["weeks"] as List<dynamic>).map((e) => e as int),
        );
        courseList.add(
          Course(
            name: course["name"] as String,
            begin: course["begin"] as int,
            classroom: course["classroom"] as String,
            dow: course["dow"] as int,
            end: course["end"] as int,
            teacher: course["teacher"] as String,
            weeks: weeks,
            color: course["color"] as int,
          ),
        );
      }
      for (Map<String, dynamic> courseTime in courseTimes) {
        courseTimeList.add(
          CourseTime(
            startHour: courseTime["startHour"] as int,
            startMinute: courseTime["startMinute"] as int,
            endHour: courseTime["endHour"] as int,
            endMinute: courseTime["endMinute"] as int,
          ),
        );
      }
    } catch (e) {
      e.printError();
    }
  }
  
  // 获取课程对应节次
  CourseTime getTime(int span) {
    if (span < 1 || span > courseTimeList.length) return courseTimeList[0];
    return courseTimeList[span - 1];
  }

  // 获取当天全部课程
  // everything: 包括非本周课程
  List<Course> getCourseOfDay(
    int currentWeekID,
    int dow, {
    bool everything = false,
  }) {
    List<Course> result = [];
    for (final course in courseList) {
      if (course.dow == dow) {
        if (course.weeks.contains(currentWeekID) || everything) {
          result.add(course);
        }
      }
    }
    return result;
  }

  // 计算“真时差”，包括天数不同的情况，此函数假设待计算课程在当天之后
  // 如果返回小于0，表示当前课程在当前时间 currentWeekID, now 之后不开课
  int realDiff(int currentWeekID, DateTime now, Course course) {
    // 相差天数计算
    int deltaDay = course.dow + 7 - now.weekday;
    if (deltaDay < 7) {
      // 目标课程（可能）在下一周，向周次借位
      currentWeekID += 1;
    } else if (deltaDay == 7) {
      // 目标课程在同一天，但可能已经上完，这里判断是否需要借位
      final courseEnd = getTime(course.end);
      if (courseEnd.diff(now) < 0) {
        // 课程已经上完
        currentWeekID += 1;
      } else {
        deltaDay -= 7;
      }
    } else {
      // 目标课程在同一周，无需借位
      deltaDay -= 7;
    }
    // 起始周次补偿，如果这节课还未开始，则需计算距离开始周的时间
    int deltaWeek = course.weeks[0] - currentWeekID;
    if (deltaWeek < 0) deltaWeek = 0;

    if (course.weeks.last < currentWeekID) return -1; // 这节课确实已结束

    List<int> deltas; // 对于每一节，计算相对当前时间（同一天）或相对0点（另一天）的时差
    if (deltaDay == 0 && deltaWeek == 0) {
      deltas = List.generate(course.end - course.begin, (idx) {
        return getTime(idx + course.begin).diff(now);
      });
    } else {
      deltas = List.generate(course.end - course.begin, (idx) {
        return getTime(idx + course.begin).startHour * 60 +
            getTime(idx + course.begin).startMinute -
            (now.hour * 60 + now.minute);
      });
    }
    int deltaWholeDay = (deltaDay + deltaWeek * 7) * 60 * 24;
    // 特殊情况：如果正在上课，直接返回0
    if (deltaWholeDay == 0 && deltas.contains(0)) return 0;

    int deltaMin = 99999999;

    for (var element in deltas) {
      deltaMin = min(deltaMin, element);
    }

    int result = deltaMin + deltaWholeDay;
    return result;
  }

  // 获取当前时间对应的课程，如果为大课间，则获取下一节课（可以是未来任一天）
  Course? getCurrentCourse(int currentWeekID, DateTime now) {
    Course? result;
    int deltaMin = 99999999;
    for (final course in courseList) {
      final delta = realDiff(currentWeekID, now, course);
      if (delta < 0) continue;
      if (delta < deltaMin) {
        deltaMin = delta;
        result = course;
      }
    }
    return result;
  }

  // 获取某一节课的下一节课（传入null表示获取当前课程的下一节课）
  Course? getNextCourseOf(int currentWeekID, DateTime now, Course? base) {
    int deltaBase;
    if (base != null) {
      deltaBase = realDiff(currentWeekID, now, base);
    } else {
      deltaBase = 0;
    }
    if (deltaBase < 0) {
      deltaBase = 0; //base课程已结束，获取当前课程的下一节
    }
    Course? result;
    int deltaMin = 99999999;
    for (final course in courseList) {
      final delta = realDiff(currentWeekID, now, course);
      if (delta <= deltaBase) continue; // 小于等于，排除同一课程或当前课程
      if (delta < deltaMin) {
        deltaMin = delta;
        result = course;
      }
    }
    return result;
  }

  // 获取当天还剩余的课程数量
  int getCourseRemaining(int currentWeekID, DateTime now) {
    int remaining = 0;
    final courseOfToday = getCourseOfDay(currentWeekID, now.weekday);
    for (var course in courseOfToday) {
      final startTime = getTime(course.begin);
      final delta = startTime.diff(now);
      if (delta <= 0 || delta > 24 * 60) continue;
      remaining += 1;
    }
    return remaining;
  }

  // 刷新闹钟
  static const channelAlarm = MethodChannel('channel.ncepu.example.com');

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();

    var json = prefs.getString("timetable");
    if (json == null || json == "") {
      exampleCourseList();
      exampleCourseTimeList();
      json = exportJSON();
      prefs.setString("timetable", json);
    }
    importJSON(json);
    channelAlarm.invokeMethod("alarm", json);
  }

  // 初始化课程数组
  // 输入保证end >= begin，且weeks按顺序排列
  void exampleCourseList() {
    courseList.addAll([
      Course(
        name: "模拟电子技术基础",
        classroom: "教1楼312",
        teacher: "张满红",
        weeks: List.generate(16 - 5 + 1, (index) => index + 5),
        dow: 1,
        begin: 1,
        end: 2,
      ),
      Course(
        name: "电路分析基础实验",
        classroom: "教5B113(电工)",
        teacher: "高春嘉",
        weeks: List.generate(18 - 15 + 1, (index) => index + 15),
        dow: 1,
        begin: 3,
        end: 4,
      ),
      Course(
        name: "算法设计与分析",
        classroom: "教3A309",
        teacher: "周长玉",
        weeks: List.generate(12 - 1 + 1, (index) => index + 1),
        dow: 1,
        begin: 5,
        end: 6,
      ),
      Course(
        name: "操作系统A",
        classroom: "教3A401",
        teacher: "贾静平",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 1,
        begin: 7,
        end: 8,
      ),
      // 周二
      Course(
        name: "汇编语言程序设计",
        classroom: "教3B214",
        teacher: "王红",
        weeks: List.generate(8 - 1 + 1, (index) => index + 1),
        dow: 2,
        begin: 1,
        end: 2,
      ),
      Course(
        name: "概率论与数理统计",
        classroom: "教3B109",
        teacher: "江登英",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 2,
        begin: 3,
        end: 4,
      ),
      Course(
        name: "模电实验",
        classroom: "教5D204（电子1）",
        teacher: "祁琪",
        weeks: List.generate(17 - 10 + 1, (index) => index + 10),
        dow: 2,
        begin: 7,
        end: 8,
      ),
      // 周三
      Course(
        name: "模拟电子技术基础",
        classroom: "教1楼312",
        teacher: "张满红",
        weeks: List.generate(16 - 5 + 1, (index) => index + 5),
        dow: 3,
        begin: 1,
        end: 2,
      ),
      Course(
        name: "体育(4)",
        classroom: "教5B511",
        teacher: "肖慧",
        weeks: List.generate(15 - 1 + 1, (index) => index + 1),
        dow: 3,
        begin: 3,
        end: 4,
      ),
      Course(
        name: "算法设计与分析",
        classroom: "教3A309",
        teacher: "周长玉",
        weeks: List.generate(12 - 1 + 1, (index) => index + 1),
        dow: 3,
        begin: 5,
        end: 6,
      ),
      Course(
        name: "电路分析基础",
        classroom: "教3A305",
        teacher: "高春嘉",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 3,
        begin: 7,
        end: 8,
      ),
      Course(
        name: "操作系统A",
        classroom: "教3A401",
        teacher: "贾静平",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 3,
        begin: 9,
        end: 10,
      ),
      // 周四
      Course(
        name: "汇编语言程序设计",
        classroom: "教3B214",
        teacher: "王红",
        weeks: List.generate(8 - 1 + 1, (index) => index + 1),
        dow: 4,
        begin: 1,
        end: 2,
      ),
      Course(
        name: "形势与政策4",
        classroom: "教3C410",
        teacher: "孙翠亭",
        weeks: List.generate(12 - 9 + 1, (index) => index + 9),
        dow: 4,
        begin: 1,
        end: 2,
      ),
      Course(
        name: "概率论与数理统计",
        classroom: "教3B109",
        teacher: "江登英",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 4,
        begin: 3,
        end: 4,
      ),
      Course(
        name: "大学生生涯规划与择业",
        classroom: "教3B205",
        teacher: "马海红",
        weeks: List.generate(10 - 1 + 1, (index) => index + 1),
        dow: 4,
        begin: 7,
        end: 8,
      ),
      // 周五
      Course(
        name: "电路分析基础",
        classroom: "教3A305",
        teacher: "高春嘉",
        weeks: List.generate(14 - 1 + 1, (index) => index + 1),
        dow: 5,
        begin: 1,
        end: 2,
      ),
    ]);
  }

  void exampleCourseTimeList() {
    courseTimeList.addAll([
      CourseTime(startHour: 8, startMinute: 0, endHour: 8, endMinute: 45),
      CourseTime(startHour: 8, startMinute: 55, endHour: 9, endMinute: 40),
      CourseTime(startHour: 10, startMinute: 0, endHour: 10, endMinute: 45),
      CourseTime(startHour: 10, startMinute: 55, endHour: 11, endMinute: 40),
      CourseTime(startHour: 14, startMinute: 0, endHour: 14, endMinute: 45),
      CourseTime(startHour: 14, startMinute: 55, endHour: 15, endMinute: 40),
      CourseTime(startHour: 16, startMinute: 0, endHour: 16, endMinute: 45),
      CourseTime(startHour: 16, startMinute: 55, endHour: 17, endMinute: 40),
      CourseTime(startHour: 19, startMinute: 0, endHour: 19, endMinute: 45),
      CourseTime(startHour: 19, startMinute: 55, endHour: 20, endMinute: 40),
    ]);
  }
}
