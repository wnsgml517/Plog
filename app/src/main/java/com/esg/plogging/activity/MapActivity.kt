package com.esg.plogging.activity

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.esg.plogging.R
import java.security.MessageDigest
import java.security.MessageDigest.*
import java.security.NoSuchAlgorithmException
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat

import com.esg.plogging.databinding.ActivityMapBinding
import com.google.android.gms.maps.MapFragment
import net.daum.mf.map.api.*
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager.removeAllPolylines

class MapActivity : AppCompatActivity(),MapView.POIItemEventListener,
MapView.MapViewEventListener {

    lateinit var binding : ActivityMapBinding
    private lateinit var mapView : MapView
    var centerPoint : MapPoint? = null
    val currentLocationMarker : MapPOIItem = MapPOIItem()
    private var pausedTime: Long = 0
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 타이머 기록 재는 부분
    private var isRecording = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var loginAccess = false
    private var distanceInMeters: Float = 0.0f
    private val timerHandler = Handler()
    private var mapViewContainer : ViewGroup? = null
    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            pausedTime +=1000
            updateTimer(pausedTime)
            timerHandler.postDelayed(this, 1000) // 1-second delay
        }
    }

    // 위치 경로 변수
    // 경로 정보 저장을 위한 리스트
    private val path: MutableList<MapPoint> = mutableListOf()

    // 경로를 그리기 위한 폴리라인 객체
    private val mapPolyline: MapPolyline = MapPolyline()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        binding = ActivityMapBinding.inflate(layoutInflater)

        binding.startButton.setOnClickListener {

            // Start button clicked, perform your action here
            binding.startButton.visibility = View.GONE
            binding.playButton.visibility = View.VISIBLE
            binding.stopButton.visibility = View.VISIBLE

            binding.timerTextView.visibility = View.VISIBLE
            binding.distanceTextView.visibility = View.VISIBLE
            // 타이머 시작
            // 여기에서 타이머를 시작하는 코드를 작성하세요.
            // 타이머가 실행 중일 때 거리 정보를 업데이트해야 할 것입니다.
            // 타이머 코드를 여기에 추가하세요.
            if (!isRecording){
                isRecording = true
                startTime = System.currentTimeMillis()
                System.out.println("시작시간 : "+startTime.toString())
                distanceInMeters = 0.0f
                timerHandler.post(timerRunnable)
            }


        }

        binding.playButton.setOnClickListener {
            // Play button clicked, perform your action here
            // Handle the action when play is clicked

            // 기록 다시 시작

            if (!isRecording) {
                // Start recording
                isRecording = true
                //startTime = System.currentTimeMillis()
                //distanceInMeters = 0.0f
                timerHandler.post(timerRunnable)
                // 진행 상태 이모지로 변경
                binding.playButton.setImageResource(R.drawable.pause_solid)
            } else {
                    // Pause recording and show dialog
                    showPauseDialog()
            }

        }

        binding.stopButton.setOnClickListener {

            // 다시 원래 배경화면으로 돌아가기...

            //binding.startButton.visibility = View.VISIBLE
            //binding.playButton.visibility = View.GONE
            //binding.stopButton.visibility = View.GONE


            //binding.timerTextView.visibility = View.GONE
            //binding.distanceTextView.visibility = View.GONE

            //isRecording = false
            //pausedTime = 0 // 0으로 초기화
            //timerHandler.removeCallbacks(timerRunnable)
            showStopDialog()
        }
        binding.userButton.setOnClickListener()
        {
            if(!loginAccess){
                // 로그인 페이지로 이동
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                // 예시 : 로그인 완료되었다 가정.
                loginAccess = true
            }
            else
            {
                // 마이 페이지로 이동
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
            }


        }

        //현재 위치로 이동
        startTracking()

        mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer!!.addView(mapView)

        setContentView(binding.root)



        // 카카오 key 값 얻기
        //getKakaoMapHashKey(this)
    }
    private fun showPauseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("일시정지 팝업")
            .setMessage("플로깅을 잠시 멈출까요?")
            .setPositiveButton("일시정지") { dialog, _ ->
                isRecording = false
                timerHandler.removeCallbacks(timerRunnable)
                dialog.dismiss()
                // 모양 변경
                binding.playButton.setImageResource(R.drawable.play_solid)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }

    private fun showStopDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("플로깅 종료 팝업")
            .setMessage("플로깅을 완료할까요?")
            .setPositiveButton("완료") { dialog, _ ->

                isRecording = false

                timerHandler.removeCallbacks(timerRunnable)

                //stop 시.
                endTime = System.currentTimeMillis()
                System.out.println("종료시간 : " + endTime.toString())
                dialog.dismiss()

                //기존 페이지로 초기화.
                binding.startButton.visibility = View.VISIBLE
                binding.playButton.visibility = View.GONE
                binding.stopButton.visibility = View.GONE


                binding.timerTextView.visibility = View.GONE
                binding.distanceTextView.visibility = View.GONE

                startTracking()

                // 기록 페이지로 이동
                // 기록 정보를 Intent에 담아서 RecordActivity로 이동
                val intent = Intent(this, RecordActivity::class.java).apply {
                    putExtra("userName", "도비") // TODO: 사용자 이름 설정, 추후 로그인 정보로 들고올 예정
                    putExtra("elapsedTime", pausedTime)
                    putExtra("distance", distanceInMeters)
                    // TODO: 이동 경로 등의 정보를 Intent에 추가
                    //putExtra("path",path)
                }
                pausedTime = 0 // 0으로 초기화
                mapViewContainer?.removeAllViews();
                startActivity(intent)

            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }
    // 현재 사용자 위치추적
    @SuppressLint("MissingPermission")
    private fun startTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading  //이 부분

        val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userNowLocation: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        //위도 , 경도
        val uLatitude = userNowLocation?.latitude
        val uLongitude = userNowLocation?.longitude
        val uNowPosition = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)


        centerPoint = uNowPosition

        // 현 위치에 마커 찍기
        val marker = MapPOIItem()
        marker.itemName = "현 위치"
        marker.mapPoint =uNowPosition
        marker.markerType = MapPOIItem.MarkerType.BluePin
        marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        mapView.addPOIItem(marker)
    }

    private fun updateTimer(elapsedTime: Long) {
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        binding.timerTextView.text = formattedTime
    }

    private fun updateDistance(newLocation: Location) {
        if (isRecording && centerPoint != null) {
            val startLocation = Location("start")
            startLocation.latitude= centerPoint!!.mapPointGeoCoord.latitude
            startLocation.longitude=centerPoint!!.mapPointGeoCoord.longitude

            val newDistance = newLocation.distanceTo(startLocation)
            distanceInMeters += newDistance
            binding.distanceTextView.text = "Distance: ${String.format("%.2f", distanceInMeters)} meters"
        }
    }

    // 위치추적 중지
    private fun stopTracking() {
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
    }

    fun onLocationChanged(location: Location?) {
        if (location != null) {
            updateDistance(location)

            // 위치가 변경될 때마다 경로 정보를 리스트에 추가
            val userPosition = MapPoint.mapPointWithGeoCoord(location.latitude, location.longitude)
            path.add(userPosition)

            // 폴리라인 업데이트
            updatePathPolyline()

            // 기타 필요한 작업 수행...
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    // 경로를 폴리라인으로 그리는 메서드
    private fun updatePathPolyline() {
        removeAllPolylines() // 기존의 폴리라인 삭제
        // 경로의 각 지점을 폴리라인에 추가
        for (point in path) {
            mapPolyline.addPoint(point)
        }

        // 폴리라인 스타일 설정
        mapPolyline.lineColor = Color.argb(128, 255, 0, 0) // 적절한 색상 및 투명도 지정
       // mapPolyline.= 5 // 폴리라인 두께 설정

        // 지도에 폴리라인 추가
        mapView.addPolyline(mapPolyline)
    }

    private fun createCustomMarker(mapView: MapView){
        val customMarker = MapPOIItem()
        customMarker.itemName = "Custom Marker"
        customMarker.tag = 1

    }

    fun getKakaoMapHashKey(context: Context) { //카카오 해시 값 얻는 함수
        try {
            val packageName = context.packageName
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in packageInfo.signatures) {
                val md = getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.d("KakaoMap Hash Key", hashKey)
            }
        } catch (e: Exception) {
            Log.e("KakaoMap Hash Key", "Error: ${e.message}")
        }
    }

    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        TODO("Not yet implemented")
    }


    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {
        TODO("Not yet implemented")
    }

    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?
    ) {
        TODO("Not yet implemented")
    }

    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewInitialized(p0: MapView?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }


}