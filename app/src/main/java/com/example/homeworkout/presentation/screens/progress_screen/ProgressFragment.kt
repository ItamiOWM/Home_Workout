package com.example.homeworkout.presentation.screens.progress_screen

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.homeworkout.*
import com.example.homeworkout.databinding.AddWeightDialogBinding
import com.example.homeworkout.databinding.EditUserDialogBinding
import com.example.homeworkout.databinding.FragmentProgressBinding
import com.example.homeworkout.domain.models.Response
import com.example.homeworkout.domain.models.UserInfoModel
import com.example.homeworkout.presentation.adapters.user_info_adapter.UserInfoAdapter
import com.example.homeworkout.presentation.viewmodel_factory.WorkoutViewModelFactory
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import javax.inject.Inject


class ProgressFragment : Fragment() {


    private var _binding: FragmentProgressBinding? = null
    private val binding: FragmentProgressBinding
        get() = _binding ?: throw RuntimeException("FragmentProgressBinding is null")


    private val component by lazy {
        (requireActivity().application as AppWorkout).component
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getCountOfCompletedWorkouts()
    }

    @Inject
    lateinit var viewModelFactory: WorkoutViewModelFactory

    private lateinit var viewModel: ProgressViewModel

    @Inject
    lateinit var userInfoAdapter: UserInfoAdapter

    private lateinit var dialogAddUser: Dialog

    private lateinit var dialogEditUser: Dialog

    private lateinit var datePickerDialog: DatePickerDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var latestTmpUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)[ProgressViewModel::class.java]
        setupUserInfoAdapter()
        collectUIState()
        setupDatePicker()
        setupDialogAddUser()
        setupOnButtonAddUserInfoClickListener()
    }

    private fun collectUIState() {

        lifecycleScope.launchWhenStarted {
            viewModel.listUserInfo.collect {
                if (it is Response.Loading) {
                    binding.progressBarLoading.visibility = View.VISIBLE
                } else {
                    binding.progressBarLoading.visibility = View.GONE
                }
                when (it) {
                    is Response.Success -> {
                        userInfoAdapter.submitList(it.data)
                        setupBarChart(it.data)
                    }
                    is Response.Failed -> {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        lifecycleScope.launchWhenStarted {

            viewModel.state.collect { state ->
                if (state is Loading) {
                    binding.progressBarLoading.visibility = View.VISIBLE
                } else {
                    binding.progressBarLoading.visibility = View.GONE
                }
                when (state) {
                    is Failure -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is AddedUserInfo -> {
                        Toast.makeText(context, getString(R.string.added_scfly), Toast.LENGTH_SHORT)
                            .show()
                    }
                    is UpdatedUserInfo -> {
                        Toast.makeText(context,
                            getString(R.string.updated_scfly),
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                    is DeletedUserInfo -> {
                        Toast.makeText(context,
                            getString(R.string.deleted_scfly),
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                    is CompletedWorkouts -> {
                        binding.tvCompletedWorkouts.text = String.format(
                            getString(R.string.count_of_workout_completed, state.count.toString()))
                    }
                }
            }
        }

    }

    private fun setupUserInfoAdapter() {

        binding.rvUserInfo.adapter = userInfoAdapter

        userInfoAdapter.onItemClicked = {
            setupDialogEditUser(it)
            dialogEditUser.show()
        }

        userInfoAdapter.onItemLongClicked = {
            setupDialogEditUser(it)
            dialogEditUser.show()
        }

    }

    private fun setupBarChart(listUserInfo: List<UserInfoModel>) {
        val array: List<BarEntry> = if (listUserInfo.isNotEmpty()) {
            listUserInfo.mapIndexed { index, userInfoModel ->
                BarEntry(index.toFloat() * 10, userInfoModel.weight.toFloat())
            }
        } else {
            listOf(BarEntry(0f, 0f))
        }
        val barDataSer = BarDataSet(array, getString(R.string.weight))
        barDataSer.setDrawValues(false)
        binding.barChart.data = BarData(barDataSer)
        binding.barChart.animateY(BAR_CHART_ANIMATION_DURATION)
        binding.barChart.description.text = getString(R.string.your_weight)
        binding.barChart.description.textColor = Color.BLUE

    }

    private fun setupOnButtonAddUserInfoClickListener() {

        binding.buttonAddUserInfo.setOnClickListener {
            dialogAddUser.show()
        }

    }

    private fun setupDialogAddUser() {
        val addWeightDialogBinding = AddWeightDialogBinding.inflate(layoutInflater)
        dialogAddUser = Dialog(requireContext())
        dialogAddUser.setContentView(addWeightDialogBinding.root)
        dialogAddUser.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialogAddUser.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogAddUser.setCancelable(true)
        dialogAddUser.window?.attributes?.windowAnimations = R.style.window_animation
        setOnDialogAddUserButtonsClickListeners(addWeightDialogBinding, dialogAddUser)
    }


    private fun setOnDialogAddUserButtonsClickListeners(
        binding: AddWeightDialogBinding,
        dialog: Dialog,
    ) {
        with(binding) {

            tvAdd.setOnClickListener {
                viewModel.addUserInfo(
                    tvDate.text.toString(),
                    etWeight.text.toString(),
                    ivUserPhoto.drawable.toBitmap(200, 200, null)
                )
                dialog.hide()
            }

            ivUserPhoto.setOnClickListener {
                takeImage()
            }

            tvDate.setOnClickListener {
                datePickerDialog.show()
            }

            lifecycleScope.launchWhenStarted {
                viewModel.state.collect { state ->
                    when (state) {
                        is ImageUri -> binding.ivUserPhoto.setImageURI(state.uri)
                        is DateForProgressScreen -> binding.tvDate.text = state.date
                    }
                }
            }

        }
    }

    private val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { uri ->
                    viewModel.updateImageUri(uri)
                }
            }
        }

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.updateImageUri(uri) }
        }


    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takeImageResult.launch(uri)
            }
        }
    }

    private fun selectImageFromGallery() = selectImageFromGalleryResult.launch("image/*")

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png").apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private fun setupDialogEditUser(userInfo: UserInfoModel) {
        val editUserDialogBinding = EditUserDialogBinding.inflate(layoutInflater)
        dialogEditUser = Dialog(requireContext())
        dialogEditUser.setContentView(editUserDialogBinding.root)
        dialogEditUser.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialogEditUser.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogEditUser.setCancelable(true)
        dialogEditUser.window?.attributes?.windowAnimations = R.style.window_animation
        setOnDialogEditUserButtonsClickListeners(editUserDialogBinding, dialogEditUser, userInfo)
    }

    private fun setOnDialogEditUserButtonsClickListeners(
        binding: EditUserDialogBinding,
        dialog: Dialog,
        userInfo: UserInfoModel,
    ) {
        with(binding) {

            tvEdit.setOnClickListener {
                viewModel.updateUserInfo(
                    tvDate.text.toString(),
                    etWeight.text.toString(),
                    ivUserPhoto.drawable.toBitmap(200, 200, null)
                )
                dialog.hide()
            }

            tvDelete.setOnClickListener {
                viewModel.deleteUserInfo(userInfo)
                dialog.hide()
            }

            ivUserPhoto.setImageBitmap(fromByteArrayToBitmap(userInfo.photo))

            ivUserPhoto.setOnClickListener {
                takeImage()
            }

            etWeight.setText(userInfo.weight)

            tvDate.text = userInfo.date

            lifecycleScope.launchWhenStarted {
                viewModel.state.collect { state ->
                    if (state is ImageUri) {
                        binding.ivUserPhoto.setImageURI(state.uri)
                    }
                }
            }

        }
    }

    private fun setupDatePicker() {

        datePickerDialog = DatePickerDialog(requireContext())

        datePickerDialog.datePicker.setOnDateChangedListener { datePicker, year, month, day ->
            val date = formatDateFromDatePicker(day, month, year)
            viewModel.updateDate(date)
        }

    }
}