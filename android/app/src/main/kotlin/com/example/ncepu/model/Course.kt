package com.example.ncepu.model

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val name: String,
    val classroom: String,
    val teacher: String,
    val weeks: List<Int>,
    val dow: Int,
    val begin: Int,
    val end: Int,
    val color: Int = 0,
);