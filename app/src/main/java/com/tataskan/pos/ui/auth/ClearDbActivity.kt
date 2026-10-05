package com.tataskan.pos.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tataskan.pos.TataskanApplication
import kotlinx.coroutines.launch

class ClearDbActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val app = application as TataskanApplication
            app.database.clearAllTables()
            finish()
        }
    }
}
