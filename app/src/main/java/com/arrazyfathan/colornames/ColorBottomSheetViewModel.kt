package com.arrazyfathan.colornames

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arrazyfathan.colornames.networking.ColorApi
import com.jakewharton.rxbinding4.widget.TextViewAfterTextChangeEvent
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class ColorBottomSheetViewModel() : ViewModel() {

    val showLoadingLiveData = MutableLiveData<Boolean>()
    val colorNameLiveData = MutableLiveData<String>()
    val closestColorLiveData = MutableLiveData<String>()

    private val searchObservable = BehaviorSubject.create<String>()
    private val textChangeEventObservable = BehaviorSubject.create<TextViewAfterTextChangeEvent>()
    private val disposables = CompositeDisposable()

    init {
        textChangeEventObservable
            .debounce(1, TimeUnit.SECONDS)
            .doOnNext { event ->
                event.editable?.let { editable ->
                    if (editable.firstOrNull() != '#') {
                        editable.insert(0, "#")
                    }
                    editable.lastOrNull()?.let {
                        if (it !in '0'..'9' && it !in 'A'..'F' && it != '#') {
                            editable.delete(editable.length - 1, editable.length)
                        }
                    }
                }
            }
            .subscribe()
            .addTo(disposables)

        val colorObservable = searchObservable
            .filter {
                it.length == 7
            }
            .flatMapSingle {
                ColorApi.getClosestColor(it)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe { showLoadingLiveData.postValue(true) }
                    .doAfterSuccess { showLoadingLiveData.postValue(false) }
            }
            .map {
                it.name
            }
            .share()

        colorObservable
            .subscribe {
                colorNameLiveData.postValue(it.value)
            }
            .addTo(disposables)

        colorObservable
            .subscribe {
                closestColorLiveData.postValue(it.closest_named_hex)
            }
            .addTo(disposables)
    }

    fun onTextChange(text: String) = searchObservable.onNext(text)

    fun afterTextChange(effect: TextViewAfterTextChangeEvent) =
        textChangeEventObservable.onNext(effect)

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
