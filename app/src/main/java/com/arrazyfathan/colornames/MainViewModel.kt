package com.arrazyfathan.colornames

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arrazyfathan.colornames.networking.ColorApi
import com.arrazyfathan.colornames.receiver.AlarmReceiver
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Created by Ar Razy Fathan Rabbani on 08/12/22.
 */
class MainViewModel(
    backgroundScheduler: Scheduler,
    mainScheduler: Scheduler,
    colorCoordinator: ColorCoordinator,
    colorApi: ColorApi,
    private val app: Application
) : AndroidViewModel(app) {

    private val disposable = CompositeDisposable()

    val hexStringSubject = BehaviorSubject.createDefault("#")
    val hexStringLiveData = MutableLiveData<String>()
    val backgroundColorLiveData = MutableLiveData<Int>()
    val rgbStringLiveData = MutableLiveData<String>()
    val rgbLiveData = MutableLiveData<RGBColor>()
    val colorNameLiveData = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    private val clearStream = PublishSubject.create<Unit>()
    private val backStream = PublishSubject.create<Unit>()
    private val digitStream = BehaviorSubject.create<String>()

    private val REQUEST_CODE = 0
    private val timeLenght: Int
    private val notifyPendingIntent: PendingIntent
    private val notifyIntent = Intent(app, AlarmReceiver::class.java)

    init {
        hexStringSubject
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribe(hexStringLiveData::postValue)
            .addTo(disposable)

        hexStringSubject
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .map { if (it.length < 7) "#000000" else it }
            .map { colorCoordinator.parseColor(it) }
            .subscribe(backgroundColorLiveData::postValue)
            .addTo(disposable)

        hexStringSubject
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .filter { it.length < 7 }
            .map { "--" }
            .subscribe(colorNameLiveData::postValue)
            .addTo(disposable)

        hexStringSubject
            .filter {
                it.length == 7
            }
            .observeOn(mainScheduler)
            .flatMapSingle {
                colorApi.getClosestColor(it)
                    .subscribeOn(backgroundScheduler)
                    .doOnSubscribe { isLoading.postValue(true) }
                    .doAfterSuccess { isLoading.postValue(false) }
            }
            .map {
                it.name.value
            }
            .subscribe(colorNameLiveData::postValue)
            .addTo(disposable)

        hexStringSubject
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .map {
                if (it.length == 7) {
                    colorCoordinator.parseRgbColor(it)
                } else {
                    RGBColor(0, 0, 0)
                }
            }
            .map { "${it.red},${it.green},${it.blue}" }
            .subscribe(rgbStringLiveData::postValue)
            .addTo(disposable)

        hexStringSubject
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .map {
                if (it.length == 7) {
                    colorCoordinator.parseRgbColor(it)
                } else {
                    RGBColor(0, 0, 0)
                }
            }
            .subscribe(rgbLiveData::postValue)
            .addTo(disposable)

        clearStream
            .map { "#" }
            .subscribe(hexStringSubject::onNext)
            .addTo(disposable)

        backStream
            .map { currentHexValue() }
            .filter {
                it.length >= 2
            }
            .map {
                it.substring(0, currentHexValue().lastIndex)
            }
            .subscribe(hexStringSubject::onNext)
            .addTo(disposable)

        digitStream
            .map { it to currentHexValue() }
            .filter { it.second.length < 7 }
            .map { it.second + it.first }
            .subscribe(hexStringSubject::onNext)
            .addTo(disposable)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notifyPendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                REQUEST_CODE,
                notifyIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            notifyPendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                REQUEST_CODE,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        timeLenght = 6000

        startTimer()
    }

    private fun currentHexValue(): String {
        return hexStringSubject.value ?: ""
    }

    fun backClicked() = backStream.onNext(Unit)

    fun clearClicked() = clearStream.onNext(Unit)

    fun digitClicked(digit: String) = digitStream.onNext(digit)

    private fun startTimer() {
        val triggeredTime = SystemClock.elapsedRealtime() + timeLenght
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 6000,
            AlarmManager.INTERVAL_HOUR,
            notifyPendingIntent
        )
    }

    fun setDigitStream(digit: String) = digitStream.onNext(digit)

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
