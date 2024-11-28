package com.example.controlward

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.controlward.ui.LoadingScreen
import com.example.controlward.ui.theme.ControlWardTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object Value {
    lateinit var uid: String
    var location = LatLng(37.5665, 126.9780)

    //    lateinit var disasterAllList: MutableList<DisasterModel>
    var disasterAllList = mutableListOf<DisasterModel>()
    val disasterMap: MutableMap<String, MutableList<DisasterModel>> = mutableMapOf(
        "Crime" to mutableListOf(),
        "EarthQuake" to mutableListOf(),
        "Flood" to mutableListOf(),
        "HeavySnow" to mutableListOf(),
        "Tsunami" to mutableListOf()
    )
    val disasterCategory = listOf(
        "Crime" to "범죄",
        "EarthQuake" to "지진",
        "Flood" to "홍수",
        "HeavySnow" to "폭설",
        "Tsunami" to "쓰나미"
    )
}

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                setContent {
                    ControlWardTheme {
                        LoadingScreen()
                    }
                }

                auth = FirebaseAuth.getInstance()
                signInAnonymously()

                lifecycleScope.launch {
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(this@MainActivity)
                    val location = getCurrentLocation(this@MainActivity, fusedLocationClient)
                    location?.let { Value.location = LatLng(it.latitude, it.longitude) }

                    getFromDB { disasters ->
                        Value.disasterAllList = disasters.toMutableList()
                        disasters.forEach { Value.disasterMap[it.category]?.add(it) }

                        setContent {
                            ControlWardTheme {
                                ScreenNavigator()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "앱을 사용하기 위해서 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        requestLocationPermission(this)
    }

    private fun requestLocationPermission(context: Context) {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocationPermission && hasCoarseLocationPermission) {
            setContent {
                ControlWardTheme {
                    ScreenNavigator()
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun signInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Value.uid = auth.currentUser?.uid ?: ""
                    }
                }
        } else {
            Value.uid = auth.currentUser?.uid ?: ""
        }
    }

    private suspend fun getCurrentLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
    ): Location? {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}
