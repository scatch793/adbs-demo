package com.omnidapt.pd.real

import android.app.Application
import com.omnidapt.pd.real.ble.BleCentralClient

class OminidaptApplication : Application() {
    val realRepository: RealRepository by lazy { RealRepository(this) }
    val bleClient: BleCentralClient by lazy { BleCentralClient(this) }
}
