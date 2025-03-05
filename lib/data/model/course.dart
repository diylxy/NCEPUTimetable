class Course {
  final String name;
  final String classroom;
  final String teacher;
  final List<int> weeks;
  final int dow; // 上课周次，1为周一，7为周日
  final int begin; // 开始节次
  final int end; // 截止节次
  final int color; // 显示颜色
  const Course({
    required this.name,
    required this.classroom,
    required this.teacher,
    required this.weeks,
    required this.dow,
    required this.begin,
    required this.end,
    this.color = 0,
  });

  Map<String, Object> toMap() {
    return {
      "name": name,
      "classroom": classroom,
      "teacher": teacher,
      "weeks": weeks,
      "dow": dow,
      "begin": begin,
      "end": end,
      "color": color,
    };
  }
}

class CourseTime {
  final int startHour;
  final int startMinute;
  final int endHour;
  final int endMinute;

  final int _minuteStart;
  final int _minuteEnd;

  const CourseTime({
    required this.startHour,
    required this.startMinute,
    required this.endHour,
    required this.endMinute,
  }) : _minuteStart = startHour * 60 + startMinute,
       _minuteEnd = endHour * 60 + endMinute;

  int diff(DateTime now) {
    final hour = now.hour;
    final minute = now.minute;
    final minuteNowTotal = hour * 60 + minute;
    if (minuteNowTotal < _minuteStart) return _minuteStart - minuteNowTotal;
    if (minuteNowTotal > _minuteEnd) return -(minuteNowTotal - _minuteEnd);
    return 0;
  }

  bool inRange(DateTime now) => diff(now) == 0;

  Map<String, Object> toMap() {
    return {
      "startHour": startHour,
      "startMinute": startMinute,
      "endHour": endHour,
      "endMinute": endMinute,
    };
  }
}
