package com.kliaou

import android.app.Application
import android.content.Context

class MainApplication : Application() {
    init {
        instance = this
    }
    companion object {
        private var instance: MainApplication? = null
        fun appContext(): Context {
            return instance!!.applicationContext
        }
    }
}