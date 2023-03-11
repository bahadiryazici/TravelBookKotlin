package com.example.kotlinmaps.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.kotlinmaps.R
import com.example.kotlinmaps.Singleton
import com.example.kotlinmaps.databinding.ActivityMapsBinding
import com.example.kotlinmaps.model.Place
import com.example.kotlinmaps.roomdb.PlaceDao
import com.example.kotlinmaps.roomdb.PlaceDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var permissionlauncher : ActivityResultLauncher<String>
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var sharedPreferences: SharedPreferences
    var tracKBoolean  : Boolean?=null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private lateinit var db:PlaceDatabase
    private lateinit var placeDao : PlaceDao
    val compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.example.kotlinmaps", MODE_PRIVATE)

        tracKBoolean = false

        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
            .allowMainThreadQueries()
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false


    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getIntExtra("info",0)

        if (info == 0){

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            locationManager  = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener{
                override fun onLocationChanged(p0: Location) {
                    tracKBoolean = sharedPreferences.getBoolean("t",false)
                    if (tracKBoolean == false) {
                        val userlocation = LatLng(p0.latitude,p0.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userlocation, 15F))
                        sharedPreferences.edit().putBoolean("t",true).apply()
                    }
                }
            }

            if (ContextCompat.checkSelfPermission(MapsActivity@this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity@this,android.Manifest.permission.ACCESS_FINE_LOCATION)){

                    Snackbar.make(binding.root,"Permission Needed",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //ask permission
                        permissionlauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                }else{
                    //ask permission
                    permissionlauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastlocation != null){
                    val lastuserlocation = LatLng(lastlocation.latitude,lastlocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastuserlocation,15F))

                }
                mMap.isMyLocationEnabled = true
            }
        }else{
            mMap.clear()
            binding.saveButton.visibility = View.GONE
            binding.deleteButton.visibility = View.VISIBLE

            Singleton.selectedPlace?.let {
                val latlng = LatLng(it.latitude,it.longitude)

                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15f))

                binding.placeNameText.setText(it.name)

            }

        }



    }

    private fun registerLauncher(){
        permissionlauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){
                if (ContextCompat.checkSelfPermission(MapsActivity@this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener )
                    val lastlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastlocation != null){
                        val lastuserlocation = LatLng(lastlocation.latitude,lastlocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastuserlocation,15F))
                    }
                    mMap.isMyLocationEnabled = true
                }

            }else{
                Toast.makeText(MapsActivity@this,"Permission needed",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {

        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.saveButton.isEnabled = true

    }

    fun save(view : View){


        val place = Place(binding.placeNameText.text.toString(),selectedLatitude!!,selectedLongitude!!)
        compositeDisposable.add(
            placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )

    }

    private fun handleResponse(){
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }


    fun delete(view : View){

        Singleton.selectedPlace?.let {
            compositeDisposable.add(placeDao.delete(it).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponse))
        }



    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}



