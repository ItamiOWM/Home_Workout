package com.example.homeworkout.domain.models

import android.os.Parcelable
import com.example.homeworkout.UNKNOWN_ID
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlannedWorkoutModel(
    val id: Int = UNKNOWN_ID,
    val date: String,
    val workoutModel: WorkoutModel,
    val isCompleted: Boolean
): Parcelable {
    companion object {

        const val UNKNOWN_IF_COMPLETED = false
    }
}