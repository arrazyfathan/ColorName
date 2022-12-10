package com.arrazyfathan.colornames

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arrazyfathan.colornames.networking.ColorApi
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
    colorApi: ColorApi
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val hexStringSubject = BehaviorSubject.createDefault("#")
    val hexStringLiveData = MutableLiveData<String>()
    val backgroundColorLiveData = MutableLiveData<Int>()
    val rgbStringLiveData = MutableLiveData<String>()
    val colorNameLiveData = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    private val clearStream = PublishSubject.create<Unit>()
    private val backStream = PublishSubject.create<Unit>()
    private val digitStream = BehaviorSubject.create<String>()

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
                    .doOnSubscribe{ isLoading.postValue(true)}
                    .doAfterSuccess { isLoading.postValue(false)}
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
                    RGBColor(255, 255, 255)
                }
            }
            .map { "${it.red},${it.green},${it.blue}" }
            .subscribe(rgbStringLiveData::postValue)
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
    }

    private fun currentHexValue(): String {
        return hexStringSubject.value ?: ""
    }

    fun backClicked() = backStream.onNext(Unit)

    fun clearClicked() = clearStream.onNext(Unit)

    fun digitClicked(digit: String) = digitStream.onNext(digit)

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
