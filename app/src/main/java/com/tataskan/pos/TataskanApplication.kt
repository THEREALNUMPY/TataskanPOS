package com.tataskan.pos

import android.app.Application
import android.util.Log
import com.tataskan.pos.data.local.getDatabaseBuilder
import com.tataskan.pos.data.local.setAppContext
import com.tataskan.pos.data.repository.PosRepository
import com.tataskan.pos.util.GlobalCrashHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TataskanApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.install(this)
        initFirebaseSafely()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.fromResource(this)
                if (options != null) {
                    FirebaseApp.initializeApp(this, options)
                    Log.d("TataskanApplication", "Firebase initialized successfully from resources.")
                } else {
                    val builder = FirebaseOptions.Builder()
                        .setApplicationId(getString(R.string.google_app_id))
                        .setApiKey(getString(R.string.google_api_key))
                        .setProjectId(getString(R.string.project_id))
                        .setGcmSenderId(getString(R.string.gcm_defaultSenderId))
                        .setStorageBucket(getString(R.string.google_storage_bucket))

                    FirebaseApp.initializeApp(this, builder.build())
                    Log.d("TataskanApplication", "Firebase initialized successfully with explicit options.")
                }
            }
        } catch (e: Exception) {
            Log.e("TataskanApplication", "Failed to initialize Firebase: ${e.message}", e)
        }
    }
    
    val database by lazy { 
        setAppContext(this)
        getDatabaseBuilder().build() 
    }
    
    val repository by lazy { 
        PosRepository(
            database.productDao(),
            database.transactionDao(),
            database.categoryDao(),
            database.promoDao(),
            database.stockAdjustmentDao()
        )
    }
}
