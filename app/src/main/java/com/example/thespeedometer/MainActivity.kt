package com.example.thespeedometer
 import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.anastr.speedviewlib.SpeedView

import kotlin.properties.Delegates
 class MainActivity : AppCompatActivity() {
     // Location variables
     private var locationByGps: Location? = null
     private var locationByNetwork: Location? = null

     // Handler variables
     private lateinit var mainHandler: Handler
     private lateinit var handler: Handler
     private lateinit var locationThread: HandlerThread

     // Speed variable
     private var speed: Float by Delegates.observable(0f) { _, old, new ->
         if (old != new) {
             mainHandler.post(object : Runnable {
                 override fun run() {
                     speedView.speedTo(new)
                 }
             })
         }
     }

     // Location manager
     private lateinit var locationManager: LocationManager

     // Speed view
     private lateinit var speedView: SpeedView
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)
         // Initialize speed view
         speedView = findViewById<SpeedView>(R.id.speedView)
         speedView.speedTo(0f)
         // Check for location permission
         isLocationPermissionGranted()
         // Initialize location thread
         locationThread = HandlerThread("locationThread")
         locationThread.start()
         handler = Handler(locationThread.looper)
         mainHandler = Handler(Looper.getMainLooper())
     }

     /**
      * Check if the location permission is granted
      */
     private fun isLocationPermissionGranted(): Boolean {
         return if (ActivityCompat.checkSelfPermission(
                 this,
                 android.Manifest.permission.ACCESS_COARSE_LOCATION
             ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                 this,
                 android.Manifest.permission.ACCESS_FINE_LOCATION
             ) != PackageManager.PERMISSION_GRANTED
         ) {
             ActivityCompat.requestPermissions(
                 this,
                 arrayOf(
                     android.Manifest.permission.ACCESS_FINE_LOCATION,
                     android.Manifest.permission.ACCESS_COARSE_LOCATION
                 ),
                 0
             )
             false
         } else {
             true
         }
     }

     override fun onStart() {
         super.onStart()
         // Initialize location manager
         locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
         // Check if GPS and Network are enabled
         val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
         val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
         try {
             if (hasGps) {
                 locationManager.requestLocationUpdates(
                     LocationManager.GPS_PROVIDER,
                     1000,
                     0F,
                     gpsLocationListener
                 )
             }
         } catch (e: SecurityException) {
             e.printStackTrace()
         }
         try {
             if (hasNetwork) {
                 locationManager.requestLocationUpdates(
                     LocationManager.NETWORK_PROVIDER,
                     1000,
                     0F,
                     networkLocationListener
                 )
             }
         } catch (e: SecurityException) {
             e.printStackTrace()
         }
     }

     /**
      * GPS location listener
      */
     val gpsLocationListener: LocationListener = object : LocationListener {
         override fun onLocationChanged(location: Location) {
             locationByGps = location
             //run on background thread
             handler.post(object : Runnable {
                 //Check if location by Network is null or if the accuracy of location by GPS is
                    // greater than location by Network, set the speed to location by GPS.
                    // Otherwise, set the speed to location by Network.
                 override fun run() {
                     if ((locationByNetwork == null) || ((locationByGps!!.accuracy > locationByNetwork!!.accuracy))) {
                         speed = locationByGps!!.speed
                     } else {
                         speed = locationByNetwork!!.speed
                     }
                 }
             })
         }

         @Deprecated("Deprecated in Java")
         override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
         }

         override fun onProviderEnabled(provider: String) {}
         override fun onProviderDisabled(provider: String) {}
     }

     /**
      * Network location listener
      */
     val networkLocationListener: LocationListener = object : LocationListener {
         override fun onLocationChanged(location: Location) {
             locationByNetwork = location
             //run on background thread
             handler.post {
//Check if location by GPS is null or if the accuracy of location by Network is
// greater than location by GPS, set the speed to location by Network.
// Otherwise, set the speed to location by GPS.
                 if ((locationByGps == null) || (locationByNetwork!!.accuracy > locationByGps!!.accuracy)) {
                     speed = locationByNetwork!!.speed
                 } else {
                     speed = locationByGps!!.speed
                 }
             }
         }

         @Deprecated("Deprecated in Java")
         override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
         }

         override fun onProviderEnabled(provider: String) {}
         override fun onProviderDisabled(provider: String) {}
     }
 }