package com.arrazyfathan.colornames

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.arrazyfathan.colornames.databinding.DialogColorBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class ColorBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: DialogColorBinding

    private val disposable = CompositeDisposable()

    private val viewModel: ColorBottomSheetViewModel by viewModels {
        createWithFactory {
            ColorBottomSheetViewModel()
        }
    }

    companion object {
        fun newInstance(colorString: String): ColorBottomSheet {
            val bundle = Bundle().apply {
                putString("ColorString", colorString)
            }
            return ColorBottomSheet().apply {
                arguments = bundle
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogColorBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.run {
                BottomSheetBehavior.from(this).state = BottomSheetBehavior.STATE_EXPANDED
                binding.root.minimumHeight = Resources.getSystem().displayMetrics.heightPixels
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colorString = arguments?.getString("ColorString") ?: ""
        initUI(colorString)
        subcribeObserver()
    }

    private fun initUI(colorString: String) {
        binding.hexInput.setText(colorString)

        binding.hexInput.textChanges()
            .map { it.toString() }
            .subscribe(viewModel::onTextChange)
            .addTo(disposable)

        binding.hexInput.afterTextChangeEvents()
            .subscribe(viewModel::afterTextChange)
            .addTo(disposable)
    }

    private fun subcribeObserver() {
        viewModel.showLoadingLiveData.observe(viewLifecycleOwner) {
            binding.loading.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.closestColorLiveData.observe(viewLifecycleOwner) {
            binding.closestColorHex.text = it
        }
        viewModel.colorNameLiveData.observe(viewLifecycleOwner) {
            binding.colorName.text = it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }
}
