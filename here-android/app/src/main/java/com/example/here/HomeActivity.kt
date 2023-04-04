package com.gdsc_gist.here

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.here.PermissionManager
import com.gdsc_gist.here.ui.theme.HereTheme
import com.google.android.gms.wearable.Wearable

class AlertConfigureActivity: ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)

        if(permissionManager.hasAllPermissions()) {
            Intent(this, RecodingService::class.java).also { intent ->
                applicationContext.startForegroundService(intent)
            }
        } else {
            permissionManager.requestPermissions()
        }




        setContent {
            HereTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    IconGrid()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}

@Composable
fun IconGrid() {
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF242424))) {
        item {
            IconButton(paint = painterResource(id = R.drawable.ambulance))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.fire_truck))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.police_car))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.virus))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.bomb))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.civil_defense))
        }
        item {
            IconButton(paint = painterResource(id = R.drawable.emergency_exit))
        }

    }

}

@Composable
fun IconButton(paint: Painter) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x00FFFFFF),
                    Color(0x20FFFFFF),
                )
            )
        )
    )
        {
            Image(painter = paint, contentDescription = "Green Check")
        }
}