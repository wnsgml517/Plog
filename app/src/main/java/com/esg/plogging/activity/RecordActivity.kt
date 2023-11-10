package com.esg.plogging.activity

// RecordActivity.kt

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityMapBinding
import com.esg.plogging.databinding.ActivityRecordBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView

class RecordActivity : AppCompatActivity(),MapView.POIItemEventListener,
    MapView.MapViewEventListener {

    private lateinit var binding: ActivityRecordBinding

    private lateinit var mapView : MapView
    private var mapViewContainer : ViewGroup? = null

    var centerPoint : MapPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        binding = ActivityRecordBinding.inflate(layoutInflater)

        // 여기서 기록된 정보를 가져와서 UI에 표시
        val userName = intent.getStringExtra("userName")
        val elapsedTime = intent.getLongExtra("elapsedTime", 0)
        val distance = intent.getFloatExtra("distance", 0.0f)
        val path = intent.getStringArrayExtra("path")

        // 사용자 이름 설정
        binding.userNameTextView.text = "$userName 님의 발자국"

        // 실행 시간, 거리 등 설정
        binding.elapsedTimeTextView.text = "총 소요시간\n ${formatElapsedTime(elapsedTime)}"
        binding.distanceTextView.text = "이동거리\n ${String.format("%.2f", distance)} meters"

        // TODO: 이동 경로를 보여주는 View에 대한 설정
        startTracking()

        // 감상평 입력 및 저장 버튼 이벤트 처리
        binding.saveButton.setOnClickListener {
            val feedback = binding.feedbackEditText.text.toString()
            // TODO: 감상평을 저장하거나 처리하는 로직 추가
            mapViewContainer?.removeAllViews();
            finish() // 기록 페이지를 닫음
        }
        mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer!!.addView(mapView)

        setContentView(binding.root)


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
    private fun formatElapsedTime(elapsedTime: Long): String {
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
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
