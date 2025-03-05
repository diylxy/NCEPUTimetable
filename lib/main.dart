import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:ncepu/data/controller/course_controller.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final courseController = CourseController();
  await courseController.init();
  Get.put(courseController, permanent: true);
  Get.put(HomePageController(), permanent: true);
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: '华电课程表',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const MyHomePage(),
    );
  }
}

class HomePageController extends GetxController {
  static HomePageController get to => Get.find();
}

class MyHomePage extends GetWidget<CourseController> {
  const MyHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text("第 1 周"),
        actions: [
          DatePickerWidget(),
          IconButton(onPressed: () {}, icon: Icon(Icons.menu)),
        ],
      ),
      body: TimetableWidget(show7Days: true),
    );
  }
}

class DatePickerWidget extends StatefulWidget {
  const DatePickerWidget({super.key});

  @override
  State<DatePickerWidget> createState() => _DatePickerWidgetState();
}

class _DatePickerWidgetState extends State<DatePickerWidget> {
  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: CourseController.to.selectedDate,
      firstDate: DateTime(2015, 8),
      lastDate: DateTime(2101),
    );
    if (picked != null && picked != CourseController.to.selectedDate) {
      CourseController.to.updateSelectedDate(picked);
    }
  }

  @override
  Widget build(BuildContext context) {
    return GetBuilder<CourseController>(
      builder: (controller) {
        final selectedDate = controller.selectedDate;
        return FilledButton.tonalIcon(
          icon: Icon(Icons.calendar_month),
          onPressed: () => _selectDate(context),
          label: Text(
            "${selectedDate.year}/${selectedDate.month}/${selectedDate.day}",
          ),
        );
      },
    );
  }
}

class TimetableWidget extends StatelessWidget {
  const TimetableWidget({super.key, this.show7Days = true});

  final bool show7Days;

  @override
  Widget build(BuildContext context) {
    final int columns = show7Days ? 7 : 5;
    return GetBuilder<CourseController>(
      init: CourseController(),
      initState: (_) {},
      builder: (controller) {
        return Column(
          children: [
            TableHeader(columns: columns),
            Expanded(
              child: SingleChildScrollView(
                scrollDirection: Axis.vertical,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Flexible(
                      flex: 1,
                      fit: FlexFit.tight,
                      child: Column(
                        children: [
                          ...List.generate(
                            controller.courseTimeList.length,
                            (index) => TableTimeSpanWidget(
                              section: index + 1,
                              start:
                                  "${controller.courseTimeList[index].startHour}:${controller.courseTimeList[index].startMinute.toString().padLeft(2, '0')}",
                              end:
                                  "${controller.courseTimeList[index].endHour}:${controller.courseTimeList[index].endMinute.toString().padLeft(2, '0')}",
                            ),
                          ),
                        ],
                      ),
                    ),
                    ...List.generate(
                      columns,
                      (weekday) => Flexible(
                        flex: 2,
                        fit: FlexFit.tight,
                        child: Column(
                          children: [
                            ...List.generate(
                              5,
                              (index) => TableCourseWidget(
                                count: index + 1,
                                course: "汇编语言程序设计\n@教3B214",
                                color: Color.lerp(
                                  Colors.green,
                                  Colors.blue,
                                  index / 10.0 + weekday / 5.0,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class TableCourseWidget extends StatelessWidget {
  const TableCourseWidget({
    super.key,
    required this.count,
    required this.course,
    this.color,
  });

  final int count;
  final String course;
  final Color? color;
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: timeSpanHeightUnit * 2,
      width: 1000,
      child: Badge.count(
        isLabelVisible: count != 1,
        offset: Offset(-4, 4),
        count: count,
        child: Card(
          elevation: 2,
          margin: EdgeInsets.all(2.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(4)),
          ),
          color: Color.lerp(color, Colors.white, 0.9),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: () {},
            child: Padding(
              padding: const EdgeInsets.all(2.0),
              child: Center(
                child: Text(
                  course,
                  textAlign: TextAlign.center,
                  style: TextTheme.of(context).bodyMedium!.apply(
                    color: Color.lerp(color, Colors.black, 0.3),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

const timeSpanHeightUnit = 100.0;

class TableTimeSpanWidget extends StatelessWidget {
  const TableTimeSpanWidget({
    super.key,
    required this.section,
    required this.start,
    required this.end,
  });

  final int section;
  final String start;
  final String end;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(
      context,
    ).textTheme.bodySmall!.apply(fontSizeFactor: 0.7);
    return SizedBox(
      height: timeSpanHeightUnit,
      width: 1000,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Text(section.toString()),
          Text(start, style: textTheme),
          Text(end, style: textTheme),
        ],
      ),
    );
  }
}

class TableHeader extends StatelessWidget {
  const TableHeader({super.key, required this.columns});

  final int columns;

  @override
  Widget build(BuildContext context) {
    return GetBuilder<CourseController>(
      init: CourseController(),
      initState: (_) {},
      builder: (controller) {
        return SizedBox(
          height: 55,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TableMonthHeaderElementWidget(
                month: DateTime.now().month.toString(),
              ),
              ...List.generate(columns, (weekday) {
                DateTime now = controller.selectedDate;
                DateTime date = now.add(
                  Duration(days: weekday + 1 - now.weekday),
                );
                return TableWeekHeaderElementWidget(
                  weekday: weekday,
                  date:
                      (date.day == 1) ? "${date.month}月" : date.day.toString(),
                  today: (weekday + 1) == now.weekday,
                );
              }),
            ],
          ),
        );
      },
    );
  }
}

class TableMonthHeaderElementWidget extends StatelessWidget {
  const TableMonthHeaderElementWidget({super.key, required this.month});
  final String month;
  @override
  Widget build(BuildContext context) {
    return Flexible(
      flex: 1,
      fit: FlexFit.tight,
      child: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.max,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              "$month\n月",
              style: TextTheme.of(context).titleSmall,
              textAlign: TextAlign.center,
              textWidthBasis: TextWidthBasis.longestLine,
            ),
          ],
        ),
      ),
    );
  }
}

class TableWeekHeaderElementWidget extends StatelessWidget {
  const TableWeekHeaderElementWidget({
    super.key,
    required this.weekday,
    required this.date,
    required this.today,
  });
  final int weekday;
  final String date;
  final bool today;
  static const List<String> weekChinese = ["一", "二", "三", "四", "五", "六", "日"];
  @override
  Widget build(BuildContext context) {
    return Flexible(
      flex: 2,
      fit: FlexFit.tight,
      child: Padding(
        padding: const EdgeInsets.all(2.0),
        child: Column(
          children: [
            Text(weekChinese[weekday], style: TextTheme.of(context).titleSmall),
            Visibility(
              visible: !today,
              child: Column(
                children: [
                  SizedBox(height: 6.0),
                  Text(
                    date,
                    style: TextTheme.of(context).titleSmall,
                    maxLines: 1,
                  ),
                ],
              ),
            ),
            Visibility(
              visible: today,
              child: Column(
                children: [
                  SizedBox(height: 2.0),
                  Card(
                    color: Theme.of(context).primaryColor,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4.0),
                      child: Text(
                        date,
                        style: TextTheme.of(context).titleSmall!.apply(
                          color: Theme.of(context).colorScheme.onPrimary,
                        ),
                        maxLines: 1,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
