package com.arrazyfathan.colornames.networking

import io.reactivex.rxjava3.core.Single

object ColorApi {
    const val API = "https://www.thecolorapi.com/"
    private val colorService = ColorService.create()

    fun getClosestColor(hexString: String): Single<ColorResponse> {
        return colorService.getColor(hexString)
    }
}
