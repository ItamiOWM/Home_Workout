package com.example.homeworkout.presentation.screens.progress_screen

import android.net.Uri

open class ProgressUIState

object Loading: ProgressUIState()

class Failure(val message: String): ProgressUIState()

class AddedUserInfo(val boolean: Boolean): ProgressUIState()

class UpdatedUserInfo(val boolean: Boolean): ProgressUIState()

class DeletedUserInfo(val boolean: Boolean): ProgressUIState()

class CompletedWorkouts(val count: Int): ProgressUIState()

class ImageUri(val uri: Uri): ProgressUIState()

class DateForProgressScreen(val date: String): ProgressUIState()



