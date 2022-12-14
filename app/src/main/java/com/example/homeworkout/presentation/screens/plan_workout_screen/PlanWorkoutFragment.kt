package com.example.homeworkout.presentation.screens.plan_workout_screen

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.homeworkout.AppWorkout
import com.example.homeworkout.R
import com.example.homeworkout.databinding.CustomActionDialogBinding
import com.example.homeworkout.databinding.FragmentPlanWorkoutBinding
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.presentation.adapters.workouts_adapter.WorkoutAdapter
import com.example.homeworkout.presentation.viewmodel_factory.WorkoutViewModelFactory
import com.example.homeworkout.utils.ToastUtil.Companion.makeToast
import javax.inject.Inject


class PlanWorkoutFragment : Fragment() {

    private var _binding: FragmentPlanWorkoutBinding? = null
    private val binding: FragmentPlanWorkoutBinding
        get() = _binding ?: throw RuntimeException("FragmentPlanWorkoutBinding is null")

    private val component by lazy {
        (requireActivity().application as AppWorkout).component
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    @Inject
    lateinit var viewModelFactory: WorkoutViewModelFactory

    @Inject
    lateinit var workoutAdapter: WorkoutAdapter

    private lateinit var viewModel: PlanWorkoutViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlanWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val args by navArgs<PlanWorkoutFragmentArgs>()

    private lateinit var dialog: Dialog

    private lateinit var workoutModel: WorkoutModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)[PlanWorkoutViewModel::class.java]
        setupRV()
        collectUIState()
        setupDialog()
    }

    private fun collectUIState() {

        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                if (state is Loading) {
                    binding.progressBarLoading.visibility = View.VISIBLE
                } else {
                    binding.progressBarLoading.visibility = View.GONE
                }
                when (state) {
                    is Failure -> {
                        makeToast(requireContext(), state.message)
                    }
                    is WorkoutPlanned -> {
                        makeToast(requireContext(), getString(R.string.workout_planned))
                    }
                    is ListWorkouts -> {
                        workoutAdapter.submitList(state.list)
                    }
                }
            }
        }

    }

    private fun setupRV() {
        binding.rvTrainings.adapter = workoutAdapter

        workoutAdapter.onItemClicked = {
            updateWorkoutModel(it)
            dialog.show()
        }
        workoutAdapter.onItemLongClicked = {
            viewModel.planWorkout(args.date, it)
        }
    }

    private fun setupDialog() {
        dialog = Dialog(requireContext())
        val dialogBinding = CustomActionDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.window?.attributes?.windowAnimations = R.style.window_animation
        setOnCustomDialogButtonsClickListeners(dialogBinding, dialog)

    }

    private fun setOnCustomDialogButtonsClickListeners(
        binding: CustomActionDialogBinding,
        dialog: Dialog,
    ) {
        with(binding) {

            tvCloseDialog.setOnClickListener {
                dialog.dismiss()
            }

            tvRightward.text = getString(R.string.plan_workout_caps)
            tvRightward.setOnClickListener {
                viewModel.planWorkout(args.date, workoutModel)
                dialog.dismiss()
            }

            tvLeftward.text = getString(R.string.go_to_detail)
            tvLeftward.setOnClickListener {
                dialog.dismiss()
                findNavController().navigate(
                    PlanWorkoutFragmentDirections.actionPlanWorkoutFragmentToWorkout(
                        workoutModel, null
                    )
                )
            }

        }
    }

    private fun updateWorkoutModel(workoutModel: WorkoutModel) {
        this.workoutModel = workoutModel
    }
}

