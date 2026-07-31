package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class PdvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.w("PdvApplication", "Firebase initialization skipped: ${e.message}")
        }
    }
}
