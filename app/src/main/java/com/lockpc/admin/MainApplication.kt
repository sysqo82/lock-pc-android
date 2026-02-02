package com.lockpc.admin

import android.app.Application
import android.content.Context

class MainApplication : Application() {
	companion object {
		lateinit var instance: MainApplication
			private set

		fun appContext(): Context = instance.applicationContext
	}

	override fun onCreate() {
		super.onCreate()
		instance = this
	}
}
