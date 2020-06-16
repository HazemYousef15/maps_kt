package com.example.twins.maps_kt

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest.permission
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import androidx.annotation.NonNull
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import android.support.test.orchestrator.junit.BundleJUnitUtils.getResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
//import org.junit.experimental.results.ResultMatchers.isSuccessful
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.OnCompleteListener
import android.text.method.TextKeyListener.clear
import com.google.android.gms.maps.CameraUpdateFactory





import java.nio.file.Files.size

import android.location.Geocoder
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.model.*
import java.io.IOException


class MapActivity : AppCompatActivity(),OnMapReadyCallback , GoogleApiClient.OnConnectionFailedListener{
    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.   //must implement an interface to any inhereted class
    }


    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onMapReady: map is ready")
        mMap = googleMap

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
            mMap!!.setMyLocationEnabled(true);              //mark blue dot on my current device location
            mMap!!.getUiSettings().setMyLocationButtonEnabled(false); //remove the navigation button as it conflicts with the search bar so we will make a custom one

            init()
        }

    }

    private val TAG = "MapActivity"
    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION


    private var mLocationPermissionsGranted = false
    private var mMap: GoogleMap? = null
    private lateinit var mFusedLocationProviderClient : FusedLocationProviderClient
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    private val DEFAULT_ZOOM = 15f
    private val LAT_LNG_BOUNDS = LatLngBounds(
            LatLng(-40.0, -168.0), LatLng(71.0, 136.0))

    private var mSearchText :EditText?=null;

    private var mGps: ImageView? = null

    private var mInfo: ImageView? = null
    private var mPlaceAutocompleteAdapter: PlaceAutocompleteAdapter? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    //private var mInfo: ImageView? = null
    //private var mPlacePicker: ImageView? = null
    private var mMarker:Marker?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mSearchText=findViewById<EditText>(R.id.input_search)
        mGps =  findViewById<ImageView>(R.id.ic_gps);
        mInfo =  findViewById<ImageView>(R.id.place_info);

        getLocationPermission();

    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment!!.getMapAsync(this)    // makes the map ready by calling OnMapReady

    }


    private fun init(){
        Log.d(TAG, "init: initializing");


//         mGoogleApiClient =  GoogleApiClient
//                .Builder(this)
//                .addApi(Places.GEO_DATA_API)
//                .addApi(Places.PLACE_DETECTION_API)
//                .enableAutoManage(this, this)
//                .build();
//
//
//
//        mPlaceAutocompleteAdapter =  PlaceAutocompleteAdapter(this, mGoogleApiClient,
//                LAT_LNG_BOUNDS, null);
//
//        mSearchText!!.setAdapter(mPlaceAutocompleteAdapter);

         mSearchText!!.setOnEditorActionListener { textView, actionId, keyEvent ->        //enters in setoneditorlistener  everytime editing in the search bar
             if(actionId == EditorInfo.IME_ACTION_SEARCH
                     || actionId == EditorInfo.IME_ACTION_DONE
                     || keyEvent.getAction() == KeyEvent.ACTION_DOWN              // all these conditions means if he presses enter (it probably differs from one device to another)
                     || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                 //execute our method for searching
                 geoLocate();
                 true
             }
             else {

                 false
             }
         }

        mGps!!.setOnClickListener { view ->

                Log.d(TAG, "onClick: clicked gps icon")     // when clicking on gps icon the map shows the current device location again
                getDeviceLocation()

        }
        mInfo!!.setOnClickListener{ view->

            Log.d(TAG, "onClick: clicked place info")
            try {
                if (mMarker!!.isInfoWindowShown()) {
                    mMarker!!.hideInfoWindow()
                } else {

                    mMarker!!.showInfoWindow()
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, "onClick: NullPointerException: " + e.message)
            }

        }
        hideSoftKeyboard()
    }


    private fun geoLocate() {
        Log.d(TAG, "geoLocate: geolocating")

        val searchString = mSearchText!!.text.toString()

        val geocoder = Geocoder(this)     // Geocoder is a class which has functions that are responsible for searching for a place
                                                  // these fns takes the place name or place location and returns in both an object containing many information regarding this place

        var list: List<Address> = ArrayList()
        try {
            list = geocoder.getFromLocationName(searchString, 1)     //takes name and max no of results to be returned
        } catch (e: IOException) {
            Log.e(TAG, "geoLocate: IOException: " + e.message)
        }

        if (list.size > 0) {
            val address = list[0]              // address object contains many attributes about this place

            Log.d(TAG, "geoLocate: found a location: " + address.toString())

            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            moveCamera(LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,address.getAddressLine(0))
        }
    }

    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (mLocationPermissionsGranted) {

                val location = mFusedLocationProviderClient.lastLocation     // gets the current location of the device
                location.addOnCompleteListener(this) { task->

                        if (task.isSuccessful) {                           //if the current location is getted succesfully
                            Log.d(TAG, "onComplete: found location!")
                            //Log.d(TAG, "aaaa"+task.result.latitude.toString())

                            var currentLocation: Location? = task.result    //casting the returned task_result into location object to get the LatLng

                            if(currentLocation!=null) {       //if device location is turned on
                                moveCamera(LatLng(currentLocation.latitude, currentLocation.longitude),
                                        DEFAULT_ZOOM,"CurrentLocation")
                            }
                            else{
                                Log.d(TAG, "onComplete: current location is null")
                                Toast.makeText(this, "please turn on your location", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.d(TAG, "onComplete: current location is null")
                            Toast.makeText(this, "unable to get current location", Toast.LENGTH_SHORT).show()
                        }
                    }

            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
        }

    }


    private fun moveCamera(latLng: LatLng, zoom: Float, title:String) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        mMap!!.clear()  //clear all previous markers in the map

        var snippet="Count=15 , Limit=20 , traffic state = intermediate"
        val options = MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        mMarker= mMap!!.addMarker(options)

        hideSoftKeyboard()
    }

    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.applicationContext,
                            COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {     //Permission granted if i already have access to this permession
                mLocationPermissionsGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(this,            // if no permession is granted onrequestpermessionresult begins to be called
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: called.")
        mLocationPermissionsGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {     //if the requestcode returned as the location_permession_request_code
                if (grantResults.size > 0) {
                    for (i in grantResults.indices) {           // loops on both fine and couarse
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            Log.d(TAG, "onRequestPermissionsResult: permission failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted")
                    mLocationPermissionsGranted = true
                    //initialize our map
                    initMap()
                }
            }
        }
    }
    private fun hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}
