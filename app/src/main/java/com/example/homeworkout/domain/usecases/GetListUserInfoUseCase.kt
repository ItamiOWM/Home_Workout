package com.example.homeworkout.domain.usecases

import com.example.homeworkout.domain.repository.WorkoutRepository
import javax.inject.Inject

class GetListUserInfoUseCase @Inject constructor(private val repository: WorkoutRepository)  {

    operator fun invoke() = repository.getListUserInfo()
}