package com.arrazyfathan.colornames

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.arrazyfathan.colornames.databinding.ActivityMainBinding
import com.arrazyfathan.colornames.networking.ColorApi
import com.arrazyfathan.colornames.workers.SendColorNotificationWorker
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val disposable = CompositeDisposable()

    private val viewModel: MainViewModel by viewModels {
        createWithFactory {
            MainViewModel(
                backgroundScheduler = Schedulers.io(),
                mainScheduler = AndroidSchedulers.mainThread(),
                colorCoordinator = ColorCoordinator(),
                colorApi = ColorApi,
                application
            )
        }
    }

    companion object {
        private const val TAG_SEND_COLOR_JOB = "tag_send_color_job"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        observe()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val hex = intent.getStringExtra("hex")
        if (hex != null) {
            viewModel.setDigitStream(hex)
        }
    }

    private fun setupScheduler() {
        val constraints = Constraints.Builder().apply {
            setRequiresBatteryNotLow(true)
        }.build()

        val work = PeriodicWorkRequestBuilder<SendColorNotificationWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_SEND_COLOR_JOB,
            ExistingPeriodicWorkPolicy.REPLACE,
            work
        )
    }

    private fun observe() {
        viewModel.hexStringLiveData.observe(
            this
        ) {
            binding.hex.text = it
            if (it == "#FFFFFF") {
                runOnUiThread {
                    binding.colorName.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.rgb.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.hex.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.progress.setIndicatorColor(ContextCompat.getColor(this, R.color.black))
                }
            } else {
                runOnUiThread {
                    binding.colorName.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.rgb.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.hex.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.progress.setIndicatorColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }

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
            this
        ) {
            animateColorChange(it)
        }
        viewModel.isLoading.observe(this) {
            binding.progress.isVisible = it
            if (it) {
                binding.colorName.visibility = View.INVISIBLE
            } else {
                binding.colorName.visibility = View.VISIBLE
            }
        }
        viewModel.rgbLiveData.observe(this) {
            if (it.red >= 187 && it.green >= 187 && it.blue >= 187) {
                runOnUiThread {
                    binding.colorName.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.rgb.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.hex.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.progress.setIndicatorColor(ContextCompat.getColor(this, R.color.black))
                }
            } else {
                runOnUiThread {
                    binding.colorName.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.rgb.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.hex.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.progress.setIndicatorColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }
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
