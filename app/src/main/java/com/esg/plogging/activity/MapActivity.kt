package com.esg.plogging.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.location.*
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.esg.plogging.R
import com.esg.plogging.databinding.ActivityMapBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.daum.mf.map.api.*
import net.daum.mf.map.n.api.internal.NativePOIItemMarkerManager.removeAllPOIItemMarkers
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager.removeAllPolylines
import java.io.IOException
import java.security.MessageDigest.getInstance
import java.util.*


class MapActivity : AppCompatActivity() {

    lateinit var binding : ActivityMapBinding
    private lateinit var mapView : MapView
    var centerPoint : MapPoint? = null // 첫 시작 위치



    private lateinit var locationManager: LocationManager

    val currentLocationMarker : MapPOIItem = MapPOIItem()
    private var pausedTime: Int = 0
    val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    //쓰레기통 위치 데이터
    private var trashLocationData = ArrayList<TrashLocationData>()
    //화장실 위치 데이터
    private var toiletLocationData = ArrayList<ToiletLocationData>()


    // 타이머 기록 재는 부분
    private var isRecording = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var loginAccess = false
    private var distanceInMeters: Double = 0.0
    private val timerHandler = Handler()
    private var mapViewContainer : ViewGroup? = null
    private var isTrashButtonClicked = false
    private var isToiletButtonClicked = false




    // 위치 경로 변수
    // 경로 정보 저장을 위한 리스트
    private val path: MutableList<MapPoint> = mutableListOf()

    // 사용자 trail 리스트
    var dataList: ArrayList<PrivateTrailData> = ArrayList()


    // 사용자 trail 리스트
    var PlogList: ArrayList<PloggingLogData> = ArrayList()


    // 경로를 그리기 위한 폴리라인 객체
    private val mapPolyline: MapPolyline = MapPolyline()

    private val eventListener = MarkerEventListener(this)

    private val viewevenlistener = DragEventListener(this)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView = MapView(this)

        removeAllPolylines() // 기존의 폴리라인 삭제
        removeAllPOIItemMarkers() // 마커 지우고 시작

        // LocationManager 초기화
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mapView.setMapViewEventListener(viewevenlistener)


        //트래킹 모드 설정
        mapView.currentLocationTrackingMode =
        MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading


        mapView.setPOIItemEventListener(eventListener)  // 마커 클릭 이벤트 리스너 등록

        binding = ActivityMapBinding.inflate(layoutInflater)


        //로그인 정보 불러오기
        val myApp = application as Plogger
        val loginData = myApp.loginData

        if(loginData!=null){ //로그인이 되어있는 경우. 정보 표!시!
            // 플로깅 위치를 불러와야 함.
            // 추천 플로깅 정보 읽어오기(csv 파일 미완료)
            System.out.println(loginData?.logUserID)

            //전체 PloggingLog 불러오기
            RecordApiManager.read("UserID",loginData?.logUserID, "PloggingLog") { success ->
                if (success!=null) {
                    PlogList = success
                }
                else {
                    System.out.println("플로깅!!로그1!!!")

                }
                //개인 플로그 정보(Private Trail) 읽어오기
                RecordApiManager.privateTrailRead("UserID",loginData?.logUserID, "PrivateTrail") { success ->
                    if (success!=null) {
                        // 기록 불러오기 성공 처리
                        dataList = success

                        runOnUiThread {
                            for ((index,stamp) in dataList.withIndex()) {
                                // 조건을 만족하는 LogList 필터링
                                val filteredLogs = PlogList.filter { it.locationName == stamp.locationName }
                                val customMarkerView = LayoutInflater.from(this).inflate(R.layout.plog_layout, null)
                                val imageView = customMarkerView.findViewById<ImageView>(R.id.plogImageView)


                                var visitNum = 0
                                var totalDistance = 0.00
                                var customMarkerBitmap : Bitmap? = null

                                if (filteredLogs.isNotEmpty()) {
                                    val bitmap = decodeBase64ToBitmap(filteredLogs[0].TrashStroagePhotos)

                                    imageView.setImageBitmap(bitmap) // ImageView에 비트맵 설정

                                    // PloggingDistance의 합 계산(누적 km 확인)
                                    totalDistance = filteredLogs.sumOf { it.PloggingDistance }
                                    //몇 번 방문했는지 확인
                                    visitNum = filteredLogs.size

                                    customMarkerBitmap = getBitmapFromView(customMarkerView) // 레이아웃을 비트맵으로 변환

                                }

                                // 각 마커 설정
                                val marker = MapPOIItem()
                                marker.apply {
                                    itemName = "${stamp.locationName} $visitNum ${String.format("%.2f", totalDistance)}"
                                    mapPoint = MapPoint.mapPointWithGeoCoord(stamp.latitude, stamp.longitude)
                                    markerType = MapPOIItem.MarkerType.CustomImage // 마커타입을 커스텀 마커로 지정.
                                    if(customMarkerBitmap==null)
                                    {
                                        customImageResourceId = R.drawable.plog_logo
                                    }
                                    else {
                                        customImageBitmap = customMarkerBitmap // 레이아웃 비트맵을 마커 이미지로 설정
                                    }
                                    isCustomImageAutoscale = false
                                    setCustomImageAnchor(0.5f,1.0f)
                                }
                                mapView.addPOIItem(marker)
                            }
                            Toast.makeText(this, "기록 불러오기 성공", Toast.LENGTH_SHORT).show()
                            // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                        }
                    } else {
                        // 기록 불러오기 실패 처리
                        runOnUiThread {
                            Toast.makeText(this, "기록 없음", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }
        }
        System.out.println(mapView.currentLocationTrackingMode)
        System.out.println("트래킹모드^^...")


        binding.startTextview.setOnClickListener {
            removeAllPolylines() // 기존의 폴리라인 삭제
            // 마커 지우고 시작
            removeAllPOIItemMarkers()
            // 로그인 된 경우만...
            // 시작 위치 설정
            setCurrentLocationOnMap()

            // 위치 업데이트 요청
            requestLocationUpdates()

            // Start button clicked, perform your action here
            binding.startButton.visibility = View.GONE
            binding.startLayout.visibility = View.GONE
            binding.startTextview.visibility = View.GONE

            binding.playButton.visibility = View.VISIBLE
            binding.stopButton.visibility = View.VISIBLE

            binding.nowView.visibility = View.VISIBLE
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
                distanceInMeters = 0.0
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
        binding.trashButton.setOnClickListener() {

            if (isTrashButtonClicked) {
                // 버튼이 이미 클릭된 상태일 때
                binding.trashButton.setBackgroundResource(R.drawable.circular_background)
                isTrashButtonClicked = false
            } else {
                // 버튼이 클릭되지 않은 상태일 때
                binding.trashButton.setBackgroundResource(R.drawable.circular_click_background)


                System.out.println("쓰레기통....")
                TrashApiManager.trashLocationGet(
                    "11010",
                    "RegionID",
                    "Toilet_TrashcanLocation"
                ) { success ->
                    if (success != null) {
                        isTrashButtonClicked = true
                        // 휴지통 위치 마커 불러오기
                        addMarkers(success)
                        //setMissionMark(success)
                        // 배열에 저장
                        trashLocationData=success

                        // 휴지통 성공 처리
                        runOnUiThread {
                            Toast.makeText(this, "쓰레기통 불러오기 성공", Toast.LENGTH_SHORT).show()
                            // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                        }
                    } else {
                        // 가입 실패 처리
                        runOnUiThread {
                            Toast.makeText(this, "쓰레기통 불러오기 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        binding.toiletButton.setOnClickListener()
        {
            if (isToiletButtonClicked) {
                // 버튼이 이미 클릭된 상태일 때
                binding.toiletButton.setBackgroundResource(R.drawable.circular_background)
                isToiletButtonClicked = false
            } else {
                // 버튼이 클릭되지 않은 상태일 때
                binding.trashButton.setBackgroundResource(R.drawable.circular_click_background)
                isToiletButtonClicked = true
                binding.toiletButton.setBackgroundResource(R.drawable.circular_click_background)
                // 화장실 위치 마커 불러오기
                //setMissionMark(toiletLocationData)
            }
        }
        binding.userButton.setOnClickListener()
        {
            val loginData = myApp.loginData

            if(loginData==null){
                // 로그인 페이지로 이동
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                System.out.println("왜이래....?")

            }
            else
            {
                removeAllPOIItemMarkers()
                // 로그인 정보 있을 경우, 마이 페이지로 이동
                val intent = Intent(this, MyPageActivity::class.java).apply {
                }
                startActivity(intent)
            }


        }


        //로그인이 되어있다면 플로깅 스팟 불러오기
        //if(loginAccess)
        //setMissionMark(PlogList)
        mapView.setCalloutBalloonAdapter(CustomBalloonAdapter(layoutInflater,isTrashButtonClicked, isToiletButtonClicked))  // 커스텀 말풍선 등록



        mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer!!.addView(mapView)

        setContentView(binding.root)


        // 카카오 key 값 얻기
        //getKakaoMapHashKey(this)
    }

    private fun getBitmapFromView(view: View): Bitmap {
        view.setLayoutParams(
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight())

        val bitmap = Bitmap.createBitmap(
            view.getMeasuredWidth(),
            view.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888
        )

        val c = Canvas(bitmap)
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())
        view.draw(c)
        return bitmap
    }

    val locationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            super.onStatusChanged(provider, status, extras)
            // provider의 상태가 변경될때마다 호출
            // deprecated
        }

        override fun onLocationChanged(location: Location) {
            // 위치가 변경될 때마다 호출되는 콜백
            val mapPoint = MapPoint.mapPointWithGeoCoord(location.latitude, location.longitude)
            path.add(mapPoint)

            // 폴리라인 업데이트
            updatePathPolyline()

            // 거리 업데이트
            val now = Location("now")
            now.latitude=mapPoint.mapPointGeoCoord.latitude
            now.longitude=mapPoint.mapPointGeoCoord.longitude
            updateDistance(now)

        }

        override fun onProviderEnabled(provider: String) {
            super.onProviderEnabled(provider)
            // provider가 사용 가능한 생태가 되는 순간 호출
        }

        override fun onProviderDisabled(provider: String) {
            super.onProviderDisabled(provider)
            // provider가 사용 불가능 상황이 되는 순간 호출
        }
    }
    private fun setCurrentLocationOnMap() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                val mapPoint = MapPoint.mapPointWithGeoCoord(
                    it.latitude,
                    it.longitude
                )
                mapView.setMapCenterPoint(mapPoint, true)
                centerPoint = mapPoint
                path.add(mapPoint) // 초기 시작 값 추가...
            }
        } else {
            // 권한이 없을 때 처리
            Toast.makeText(
                this,
                "위치 권한이 없어 현재 위치를 표시할 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun requestLocationUpdates() {
        try {
            // 위치 업데이트 요청
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 1초마다 업데이트
                0f, // 위치 변경에 대한 최소 거리 (미사용)
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    // 화면 클릭 이벤트 리스너
    class DragEventListener(val mapActivity: MapActivity): MapView.MapViewEventListener {

        override fun onMapViewInitialized(p0: MapView?) {
        }

        override fun onMapViewCenterPointMoved(p0: MapView?, movedCenter: MapPoint?) {

            if (mapActivity.isTrashButtonClicked) {
                mapActivity.addMarkers(mapActivity.trashLocationData)
                // 지도 중심 좌표가 이동한 경우
                System.out.println("지도 중심 변경")
            }
            System.out.println("지도 중심 변경")

        }

        override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
             if (mapActivity.isTrashButtonClicked) {
                mapActivity.addMarkers(mapActivity.trashLocationData)
                System.out.println("지도 줌 레벨 변경")
            }

        }

        override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
        }

        override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
        }

        override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
        }

        override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
        }

    }
        // 마커 클릭 이벤트 리스너
    class MarkerEventListener(val context: Context): MapView.POIItemEventListener {
        override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
            // 마커 클릭 시

            //CameraUpdateFactory.newMapPoint(poiItem.mapPoint,16.0)
            //poiItem?.mapPoint?.let { mapPoint ->
            //    mapView?.setMapCenterPoint(mapPoint, true)
            //    mapView?.zoomIn(true)
            //}
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
            // 말풍선 클릭 시 (Deprecated)
            // 이 함수도 작동하지만 그냥 아래 있는 함수에 작성하자
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {
            // 말풍선 클릭 시

            val builder = AlertDialog.Builder(context)
            val itemList = arrayOf("토스트", "마커 삭제", "취소")
            builder.setTitle("test")
            builder.setItems(itemList) { dialog, which ->
                when(which) {
                    0 -> Toast.makeText(context, "토스트", Toast.LENGTH_SHORT).show()  // 토스트
                    1 -> mapView?.removePOIItem(poiItem)    // 마커 삭제
                    2 -> dialog.dismiss()   // 대화상자 닫기
                }
            }
            builder.show()
        }

        override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
            // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
        }
    }
    // 커스텀 말풍선 클래스
    class CustomBalloonAdapter(inflater: LayoutInflater, private val isTrashButtonClicked : Boolean, private val isToiletButtonClicked : Boolean): CalloutBalloonAdapter {
        val mCalloutBalloon: View = inflater.inflate(R.layout.balloon_layout, null)
        val name: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_name)
        val distance: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_distance)
        val total: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_total)

        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // 마커 클릭 시 나오는 말풍선
            System.out.println(isTrashButtonClicked)
            if(isTrashButtonClicked || isToiletButtonClicked){
                name.text = poiItem?.itemName  // 해당 마커의 정보 이용 가능
                total.text = ""
                distance.text = ""
                return mCalloutBalloon
            }

            val date : ArrayList<String> = poiItem?.itemName?.split(" ") as ArrayList<String>

            System.out.println(date)
            System.out.println("마커야~~")
            name.text = date[0]   // 해당 마커의 정보 이용 가능
            total.text = "총 ${date[1]}회 방문"
            distance.text = date[2]+"m"
            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {
            // 말풍선 클릭 시
            name.text = "getPressedCalloutBalloon"
            return mCalloutBalloon
        }
    }
    private fun getAddressFromLocation(latitude: Double, longitude: Double, context: Context) {
        val geocoder = Geocoder(context, Locale.getDefault())

        try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1) as List<Address>

            if (addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val city = address.locality // 도시
                val subLocality = address.subLocality ?: address.subAdminArea // 구

                System.out.println(subLocality)
                System.out.println(city)
                System.out.println("지오코딩!!!!")
                Log.d("Location", "City: $city, SubLocality: $subLocality")
            } else {
                Log.d("Location", "No address found")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Location", "Error getting address")
        }
    }

    private fun showPauseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("일시정지")
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

    private fun setMissionMark(dataList: ArrayList<TrashLocationData>) {
        for (data in dataList) {
            val marker = MapPOIItem()
            marker.apply {
                itemName = data.trashName
                mapPoint = MapPoint.mapPointWithGeoCoord(data.latitude, data.longitude)
                //markerType = MapPOIItem.MarkerType.BluePin
                selectedMarkerType = MapPOIItem.MarkerType.RedPin
            }
            mapView.addPOIItem(marker)
        }
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
                binding.startLayout.visibility = View.VISIBLE
                binding.startTextview.visibility = View.VISIBLE

                binding.playButton.visibility = View.GONE
                binding.stopButton.visibility = View.GONE

                binding.nowView.visibility = View.GONE
                binding.timerTextView.visibility = View.GONE
                binding.distanceTextView.visibility = View.GONE


                // 기록 모달창 슬라이딩 윈도우

                /*val bottomSheetView = layoutInflater.inflate(R.layout.activity_save, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheetDialog.setContentView(bottomSheetView)

                bottomSheetDialog.show()*/
                val bottomSheetFragment = CustomBottomSheetFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)


                // 기록 페이지로 이동
                // 기록 정보를 Intent에 담아서 RecordActivity로 이동
                /*val intent = Intent(this, RecordActivity::class.java).apply {
                    putExtra("userName", "도비")
                    putExtra("elapsedTime", pausedTime)
                    putExtra("distance", distanceInMeters)

                    val pathString = path.joinToString(";") { mapPoint ->
                        "${mapPoint.mapPointGeoCoord.latitude},${mapPoint.mapPointGeoCoord.longitude}"
                    }
                    putExtra("path", pathString)
                    locationManager.removeUpdates(locationListener)
                }
                pausedTime = 0 // 0으로 초기화
                mapViewContainer?.removeAllViews();
                startActivity(intent)*/

            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }

    private fun recordTracking() {

        val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userNowLocation: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        //위도 , 경도
        val uLatitude = userNowLocation?.latitude
        val uLongitude = userNowLocation?.longitude
        val uNowPosition = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)

        if (uNowPosition != null) {
            //centerPoint=location
            //currentLocationMarker.moveWithAnimation(uNowPosition,false)
            //currentLocationMarker.alpha=1f
            //if(mapView != null){
            //    mapView.setMapCenterPoint(
            //        MapPoint.mapPointWithGeoCoord(
            //            uNowPosition.mapPointGeoCoord.latitude,
            //            uNowPosition.mapPointGeoCoord.longitude
            //        ), false
            //    )
            //}
            val now = Location("now")
            now.latitude=uNowPosition.mapPointGeoCoord.latitude
            now.longitude=uNowPosition.mapPointGeoCoord.longitude

            updateDistance(now)

            // 위치가 변경될 때마다 경로 정보를 리스트에 추가
            val userPosition = MapPoint.mapPointWithGeoCoord(uNowPosition.mapPointGeoCoord.latitude,
                uNowPosition.mapPointGeoCoord.longitude)
            path.add(userPosition)

            // 폴리라인 업데이트
            updatePathPolyline()

            // 기타 필요한 작업 수행...
        }


        System.out.println(uLatitude.toString()+uLongitude.toString()+"하하하하")



    }

    private fun updateTimer(elapsedTime: Int) {
        val seconds = (elapsedTime / 1000)
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

            //기준점 변경
            centerPoint = MapPoint.mapPointWithGeoCoord(newLocation.latitude,newLocation.longitude)

            distanceInMeters += newDistance
            binding.distanceTextView.text = "${String.format("%.2f", distanceInMeters)}m"
        }
    }

    // 위치추적 중지
    private fun stopTracking() {
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
    }



    override fun onDestroy() {
        super.onDestroy()
        //stopTracking()
    }

    // 경로를 폴리라인으로 그리는 메서드
    private fun updatePathPolyline() {
        removeAllPolylines() // 기존의 폴리라인 삭제
        // 경로의 각 지점을 폴리라인에 추가
        for (point in path) {
            mapPolyline.addPoint(point)
        }

        // 폴리라인 스타일 설정
        mapPolyline.lineColor = Color.argb(128, 0, 255, 0) // 적절한 색상 및 투명도 지정
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
    //문자->이미지 형태로 바꾸기
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        try {
            // 문자 바꾸기(POST 방식으로 값 읽고 쓰면서 값이 바뀌어서 저장됨)
            val cleanBase64 = base64String.replace(" ", "+").replace("_", "/").replace("-","+")

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            pausedTime +=1000
            updateTimer(pausedTime)
            timerHandler.postDelayed(this, 1000) // 1-second delay
        }
    }
    private fun addMarkers(trashLocationList: ArrayList<TrashLocationData>) {
        removeAllPOIItemMarkers()
        val mapBounds = mapView.mapPointBounds
        val list = trashLocationList.filter {
            mapBounds.bottomLeft.mapPointGeoCoord.longitude < it.longitude.toDouble() &&
                    mapBounds.bottomLeft.mapPointGeoCoord.latitude < it.latitude.toDouble() &&
                    mapBounds.topRight.mapPointGeoCoord.longitude > it.longitude.toDouble() &&
                    mapBounds.topRight.mapPointGeoCoord.latitude > it.latitude.toDouble()
        }
        list.forEach { addMarker(it) }

    }
    private fun addMarker(trashLocation: TrashLocationData) {
        val latitude = trashLocation.latitude.toDouble()
        val longitude = trashLocation.longitude.toDouble()

        val trashPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
        val marker = MapPOIItem()

        marker.apply {
            itemName = trashLocation.trashName
            mapPoint = trashPoint
            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
            customImageResourceId = R.drawable.trash_marker
            isCustomImageAutoscale = false
            setCustomImageAnchor(0.5f,1.0f)
            showAnimationType=MapPOIItem.ShowAnimationType.SpringFromGround
        }

        mapView.addPOIItem(marker)
    }
}
