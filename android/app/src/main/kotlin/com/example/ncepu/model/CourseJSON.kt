package com.example.ncepu.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseJSON(
    val courses: List<Course>,
    val courseTimes: List<CourseTime>,
);
