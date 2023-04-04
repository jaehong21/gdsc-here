package com.gdsc_gist.here

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.util.Log
import com.smartlook.android.core.api.Smartlook




class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val smartlook = Smartlook.instance
        smartlook.preferences.projectKey = "SMARTLOOK_PROJECT_KEY" // removed for submission

        smartlook.start()

        val sharedPreferences = getSharedPreferences("com.gdsc_gist.here", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", null)

        Log.d("MainActivity", "Name: $name")
        //TODO: check if the user has already set a name
        if (name == null) {
            startActivity(Intent(this, InitialActivity::class.java))
        } else {
            startActivity(Intent(this, AlertConfigureActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }


}
