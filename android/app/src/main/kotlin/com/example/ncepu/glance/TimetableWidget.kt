package com.example.ncepu.glance
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.ncepu.MainActivity
import com.example.ncepu.R

class TimetableWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Log.d("TimetableWidget", "小组件正在更新")
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val mainShowEmpty = prefs.getBoolean("mainShowEmpty", false)            // 第一行显示strEmpty而不是下一节课
            val hasNextClass = prefs.getBoolean("hasNextClass", true)               // 第二行显示无更多课程而不是下一节课
            val strDate = prefs.getString("strDate", "")
            val strWeek = prefs.getString("strWeek", "")
            val strTime = prefs.getString("strTime", "")
            val strClassroom = prefs.getString("strClassroom", "")
            val strCourse = prefs.getString("strCourse", "")
            val strDoW = prefs.getString("strDoW", "")
            val strCourseRemaining = prefs.getString("strCourseRemaining", "")
            val strNextTime = prefs.getString("strNextTime", "")
            val strNextCourse = prefs.getString("strNextCourse", "")
            val strNextClassroom = prefs.getString("strNextClassroom", "")
            val strEmpty = prefs.getString("strEmpty", "")
            GlanceTheme {
                Scaffold(
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                    backgroundColor = GlanceTheme.colors.widgetBackground,
                ) {
                    TimetableContent(
                        mainShowEmpty,
                        hasNextClass,
                        strDate!!,
                        strWeek!!,
                        strTime!!,
                        strClassroom!!,
                        strCourse!!,
                        strDoW!!,
                        strCourseRemaining!!,
                        strNextTime!!,
                        strNextCourse!!,
                        strNextClassroom!!,
                        strEmpty!!,
                    )
                }
            }
        }
    }

    @Composable
    private fun TimetableContent(
        mainShowEmpty: Boolean,
        hasNextClass: Boolean,
        strDate: String,
        strWeek: String,
        strTime: String,
        strClassroom: String,
        strCourse: String,
        strDoW: String,
        strCourseRemaining: String,
        strNextTime: String,
        strNextCourse: String,
        strNextClassroom: String,
        strEmpty: String,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            DateWeekLayout(
                strDate,
                strWeek
            )
            if (mainShowEmpty) {
                ThisCourseEmptyLayout(
                    strEmpty, if( strEmpty.indexOf('\n') <= 0 ) 1 else 2
                )
            }
            else {
                ThisCourseLayout(
                    strTime,
                    strClassroom,
                    strCourse
                )
            }
            Row (verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxHeight()) {
                Row(
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.padding(bottom = 12.dp),
                ) {
                    DoWLayout(strDoW)
                    if(hasNextClass) {
                        NextCourseLayout(
                            strCourseRemaining,
                            strNextTime,
                            strNextCourse,
                            strNextClassroom
                        )
                    }
                    else {
                        NextCourseEmptyLayout()
                    }
                }
            }
        }
    }
    @Composable
    private fun DateWeekLayout(
        strDate: String,
        strWeek: String,
    )
    {
        Box (
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ){
            Text(strDate,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(textAlign = TextAlign.Start)
            )
            Text(strWeek,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(textAlign = TextAlign.End)
            )
        }
    }

    @Composable
    private fun ThisCourseLayout(
        strTime: String,
        strClassroom: String,
        strCourse: String){
        Text(strTime,
            maxLines = 1,
            style = TextStyle(
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            ),
        )
        Text(strClassroom,
            maxLines = 1,
            style = TextStyle(
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
        )
        Text(strCourse,
            maxLines = 1,
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = ColorProvider(Color(0xFF454545)),
            ),
        )
    }
    @Composable
    private fun ThisCourseEmptyLayout(strEmpty: String, rows: Int) {
        val singleRowSpacing = 24.dp
        val multiRowSpacing = 2.dp
        Text(strEmpty,
            modifier = GlanceModifier.padding(vertical = if (rows == 2) multiRowSpacing else singleRowSpacing),
            style = TextStyle(
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
        )
    }
    @Composable
    private fun NextCourseLayout(
        strCourseRemaining: String,
        strNextTime: String,
        strNextCourse: String,
        strNextClassroom: String) {
        Column(
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            Text(
                strCourseRemaining,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFF454545))
                ),
            )
            Text(
                strNextTime,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                ),
            )
            Text(
                strNextCourse,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFF454545))
                ),
            )
            Text(
                strNextClassroom,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
    @Composable
    private fun NextCourseEmptyLayout(
        show: Boolean = true
    ) {
        Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            if (show) {
                Text(
                    "无更多课程",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFF454545))
                    ),
                )
            }
        }
    }
    @Composable
    private fun DoWLayout(strDoW: String) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(ImageProvider(R.drawable.calendar_icon), "dow")
            Text(
                strDoW,
                style = TextStyle(
                    fontSize = 14.sp,
                ),
                modifier = GlanceModifier.padding(top = 16.dp),
            )
        }
    }
}
