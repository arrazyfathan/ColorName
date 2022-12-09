package com.arrazyfathan.colornames

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import com.arrazyfathan.colornames.databinding.ActivityMainBinding
import com.arrazyfathan.colornames.networking.ColorApi
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val disposable = CompositeDisposable()

    private val viewModel: MainViewModel by viewModels {
        createWithFactory {
            MainViewModel(
                backgroundScheduler = Schedulers.io(),
                mainScheduler = AndroidSchedulers.mainThread(),
                colorCoordinator = ColorCoordinator(),
                colorApi = ColorApi
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        observe()
    }

    private fun observe() {
        viewModel.hexStringLiveData.observe(
            this,
            Observer {
                binding.hex.text = it
            }
        )

        viewModel.rgbStringLiveData.observe(
            this,
            Observer {
                binding.rgb.text = it
            }
        )
        viewModel.colorNameLiveData.observe(
            this,
            Observer {
                binding.colorName.text = it
            }
        )
        viewModel.backgroundColorLiveData.observe(
            this,
            Observer {
                animateColorChange(it)
            }
        )
    }

    private fun initUI() = with(binding) {
        clear.clicks().subscribe { viewModel.clearClicked() }
            .addTo(disposable)

        back.clicks().subscribe { viewModel.backClicked() }
            .addTo(disposable)

        val digits =
            listOf(zero, one, two, three, four, five, six, seven, eight, nine, A, B, C, D, E, F)
                .map { digit ->
                    digit.clicks().map { digit.text.toString() }
                }

        val digitStream = Observable.merge(digits)

        digitStream.subscribe(viewModel::digitClicked)
            .addTo(disposable)

        colorName.clicks()
            .subscribe {
                val bottomSheetDialog = ColorBottomSheet.newInstance(hex.text.toString())
                bottomSheetDialog.show(supportFragmentManager, "Custom Bottom Sheet Dialog")
            }
            .addTo(disposable)
    }

    private fun animateColorChange(newColor: Int) {
        val colorFrom = binding.rootLayout.background as ColorDrawable
        colorAnimator(colorFrom.color, newColor)
            .subscribe { color ->
                binding.rootLayout.setBackgroundColor(color)
                window.statusBarColor = color
            }
            .addTo(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }
}
