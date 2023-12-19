package com.esg.plogging.activity

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.*
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.esg.plogging.R
import com.esg.plogging.databinding.ActivityMapBinding
import com.google.android.gms.maps.model.LatLng
import net.daum.mf.map.api.*
import net.daum.mf.map.n.api.internal.NativePOIItemMarkerManager.removeAllPOIItemMarkers
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager.removeAllPolylines
import java.io.IOException
import java.security.MessageDigest.getInstance
import java.util.*


class MapActivity : AppCompatActivity(), CustomBottomSheetFragment.BottomSheetListener,
    CustomBottomSheetPostFragment.BottomSheetPostListener {

    lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    var centerPoint: MapPoint? = null // 첫 시작 위치
    var postPoint: MapPoint? = null // 제보 위치
    var postMarker: MapPOIItem? = null // 제보 마커
    var postAddress: String? = null // 제보 주소
    var postregionID: Int? = 0 // 제보 regionID
    var recordregionID: Int? = 0 // 기록 위치 regionID


    private lateinit var locationManager: LocationManager

    val currentLocationMarker: MapPOIItem = MapPOIItem()
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
    private var mapViewContainer: ViewGroup? = null
    private var isTrashButtonClicked = false
    private var isToiletButtonClicked = false
    var isPostButtonClicked = false


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

        // MapView의 초기 확대 수준을 조절합니다.
        mapView.setZoomLevel(1, true) // 여기서 14는 초기 확대 수준입니다.


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

        if (loginData != null) { //로그인이 되어있는 경우. 정보 표!시!
            // 플로깅 위치를 불러와야 함.
            // 추천 플로깅 정보 읽어오기(csv 파일 미완료)
            System.out.println("첫 시작")

            updateMyTrailLog(loginData.logUserID!!)

        }
        System.out.println(mapView.currentLocationTrackingMode)
        System.out.println("트래킹모드^^...")

        binding.resetButton.setOnClickListener(){
            //기존 페이지로 초기화.
            binding.startButton.visibility = View.VISIBLE
            binding.startLayout.visibility = View.VISIBLE
            binding.startTextview.visibility = View.VISIBLE

            binding.playButton.visibility = View.GONE
            binding.stopButton.visibility = View.GONE

            binding.nowView.visibility = View.GONE
            binding.timerTextView.visibility = View.GONE
            binding.distanceTextView.visibility = View.GONE

            //경로랑 마커 삭제
            removeAllPolylines()
            removeAllPOIItemMarkers()

            // 타이머 중지
            isRecording = false

            timerHandler.removeCallbacks(timerRunnable)

        }
        binding.startTextview.setOnClickListener {
            // 로그인 정보 불러오기
            val loginData = myApp.loginData
            // 로그인이 안되었을 경우,,,,로그인하러 이동
            if (loginData == null) {
                // 로그인 페이지로 이동
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
            } else {
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

                if (!isRecording) {
                    isRecording = true
                    startTime = System.currentTimeMillis()
                    System.out.println("시작시간 : " + startTime.toString())
                    distanceInMeters = 0.0
                    timerHandler.post(timerRunnable)
                }
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
                // 버튼이 이미 클릭된 상태일 때 (끄고 싶은 것)

                // 트래킹 모드 다시 시작
                mapView.currentLocationTrackingMode =
                    MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading

                binding.trashButton.setBackgroundResource(R.drawable.circular_background)
                isTrashButtonClicked = false
            } else {
                // 버튼이 클릭되지 않은 상태일 때 (키고 싶은 것)
                // 트래킹 모드 중지
                stopTracking()
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
                        trashLocationData = success

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

            if (loginData == null) {
                // 로그인 페이지로 이동
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
                System.out.println("왜이래....?")

            } else {
                removeAllPOIItemMarkers() // 모든 마커 삭제
                // 로그인 정보 있을 경우, 마이 페이지로 이동
                val intent = Intent(this, MyPageActivity::class.java).apply {
                }
                startActivity(intent)
            }


        }

        // 현 지도에서 위치 재검색 버튼 클릭한 경우
        binding.updateLayout.setOnClickListener() {
            // 지금 현재 보여지는 좌표 위치를 기준으로 ... 마커 추가!
            addMarkers(trashLocationData)
            // 위치 재검색 버튼 안보여지게 하기...
            binding.updateLayout.visibility = View.GONE
        }

        // 지도에서 위치 제보하기 버튼 클릭한 경우
        binding.postButton.setOnClickListener() {

            if (isPostButtonClicked) {
                // 버튼이 이미 클릭된 상태에서 클릭했을 때.(제보하기 종료)

                // 기존 중심 좌표 마커 삭제
                mapView.removePOIItem(postMarker)

                // 위치 제보 뷰 없애기
                binding.postView.visibility = View.GONE
                // 배경 원래대로 바꾸기
                binding.postButton.setBackgroundResource(R.drawable.circular_background)
                isPostButtonClicked = false
                // 트래킹 모드 다시 시작
                mapView.currentLocationTrackingMode =
                    MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading

            } else if (myApp.loginData == null) {
                // 로그인 해야 제보 가능
                val intent = Intent(this, LoginActivity::class.java).apply {
                }
                startActivity(intent)
            } else {

                // 제보하기 버튼 안 눌렀는데 눌렀음, 위치 제보 다이알로그 ...
                val dialog = Dialog(this, R.style.CustomDialogTheme)
                dialog.setContentView(R.layout.custom_dialog_layout)

                // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
                val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
                titleTextView.text = "위치 제보"
                titleTextView.gravity = Gravity.CENTER

                // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
                val infromTextView = dialog.findViewById<TextView>(R.id.dialog_inform)
                infromTextView.text = "쓰레기통 혹은 화장실 위치를\n제보하시겠습니까?"
                infromTextView.gravity = Gravity.CENTER

                // 취소 및 완료 버튼을 가운데로 정렬하고 녹색으로 변경
                val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
                val completeButton = dialog.findViewById<Button>(R.id.complete_button)

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                completeButton.setOnClickListener {
                    // 트래킹 모드 중지
                    stopTracking()

                    // 플로깅 기록 중인 경우
                    if (isRecording) {
                        //일시 정지
                        isRecording = false
                        timerHandler.removeCallbacks(timerRunnable)
                        dialog.dismiss()

                        // 모양 변경
                        binding.playButton.setImageResource(R.drawable.play_solid)
                    }

                    // 버튼이 클릭되지 않은 상태일 때(제보하기 시작)

                    // 현재 지도 중심 좌표 알아내기
                    postPoint = mapView.getMapCenterPoint()

                    // 마커 추가
                    createpostMarker(postPoint!!, mapView)

                    //배경색 연두색으로 바꾸기
                    binding.postButton.setBackgroundResource(R.drawable.circular_click_background)
                    isPostButtonClicked = true

                    // 위치 제보 뷰 보여지기
                    binding.postView.visibility = View.VISIBLE

                    // 위치 제보 뷰 이 위치로 하기 버 눌렀을 경우. 다이얼로그 보여지기
                    binding.postSaveLayout.setOnClickListener() {

                        val bottomSheetPostFragment = CustomBottomSheetPostFragment.newInstance(
                            postMarker!!.mapPoint.mapPointGeoCoord.longitude,
                            postMarker!!.mapPoint.mapPointGeoCoord.latitude,
                            34012,
                            this
                        )
                        System.out.println("태그태그")
                        System.out.println(postregionID)

                        bottomSheetPostFragment.setStyle(
                            DialogFragment.STYLE_NORMAL,
                            R.style.AppBottomSheetDialogTheme
                        )
                        bottomSheetPostFragment.show(
                            supportFragmentManager,
                            bottomSheetPostFragment.tag
                        )

                    }
                    //다이알로그 닫기
                    dialog.dismiss()
                }

                dialog.setCancelable(false)
                dialog.show()


            }
        }


        //로그인이 되어있다면 플로깅 스팟 불러오기
        //if(loginAccess)
        //setMissionMark(PlogList)
        mapView.setCalloutBalloonAdapter(
            CustomBalloonAdapter(
                this,
                layoutInflater,
                isTrashButtonClicked,
                isToiletButtonClicked,
                supportFragmentManager
            )
        )  // 커스텀 말풍선 등록


        mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer!!.addView(mapView)

        setContentView(binding.root)


        // 카카오 key 값 얻기
        //getKakaoMapHashKey(this)
    }
    fun updateMyTrailLog(id: String){
        // 개인 플로그 정보(Private Trail) 읽어오기
        RecordApiManager.privateTrailRead(
            "UserID",
            id,
            "PrivateTrail"
        ) { success ->

            if (success != null) {
                System.out.println(success)
                dataList = success
                System.out.println("길이??")
                System.out.println(dataList.size)

                // 기록 불러오기 성공 처리
                for ((index, stamp) in dataList.withIndex()) {
                    // 조건을 만족하는 LogList 필터링
                    runOnUiThread {
                        val customMarkerView =
                            LayoutInflater.from(this).inflate(R.layout.plog_layout, null)

                        val imageView =
                            customMarkerView.findViewById<ImageView>(R.id.plogImageView)
                        var customMarkerBitmap: Bitmap? = null

                        if (stamp != null) {
                            System.out.println("돼??")
                            var bitmap = decodeBase64ToBitmap(stamp.TrashStroagePhotos)
                            bitmap = getRoundedCornerBitmap(bitmap!!, 35)

                            System.out.println(bitmap)
                            imageView.setImageBitmap(bitmap) // ImageView에 비트맵 설정
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                            customMarkerBitmap =
                                getBitmapFromView(customMarkerView) // 레이아웃을 비트맵으로 변환
                        }

                        // 각 마커 설정
                        val marker = MapPOIItem()
                        marker.apply {
                            itemName = "${stamp.locationName};${stamp.totalVisit};${
                                String.format(
                                    "%.1f",
                                    stamp.totalDistance
                                )
                            }"
                            mapPoint =
                                MapPoint.mapPointWithGeoCoord(stamp.latitude, stamp.longitude)
                            markerType = MapPOIItem.MarkerType.CustomImage // 마커타입을 커스텀 마커로 지정.
                            System.out.println(customMarkerView)

                            if (customMarkerBitmap == null) {
                                customImageResourceId = R.drawable.plog_logo
                            } else {
                                customImageBitmap = customMarkerBitmap // 레이아웃 비트맵을 마커 이미지로 설정
                            }
                            isCustomImageAutoscale = false
                            setCustomImageAnchor(0.5f, 1.0f)
                        }
                        System.out.println("마커야!!!!!!")
                        System.out.println(marker.itemName)
                        mapView.addPOIItem(marker)

                        // 모든 데이터가 로드되고 마지막 stamp를 처리한 경우
                        if (index == dataList.size - 1) {
                            // 추가 작업을 여기에 추가...
                            Toast.makeText(this, "데이터가 성공적으로 로드되었습니다", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }
    override fun onBottomSheetPostDismissed(data: Boolean) {
        System.out.println(data)
        System.out.println("제보 잘 했나.")

        // 위치 제보 뷰 없애기
        binding.postView.visibility = View.GONE

        // 트래킹 모드 다시 시작
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading

        // 위치 제보 마커 없애기
        // 기존 중심 좌표 마커 삭제
        mapView.removePOIItem(postMarker)

        // 색깔 흰색으로 바꾸기 : 초기화
        binding.postButton.setBackgroundResource(R.drawable.circular_background)
        isPostButtonClicked = false

    }

    fun createpostMarker(postPoint: MapPoint, mapView: MapView) {
        if (postMarker != null) {
            // 기존 중심 좌표 마커 삭제
            mapView.removePOIItem(postMarker)
        }


        // 중심 좌표 마커 그리기
        postMarker = MapPOIItem()
        postMarker?.apply {
            itemName = postAddress
            tag = 34012
            mapPoint = postPoint
            markerType = MapPOIItem.MarkerType.CustomImage // 마커타입을 커스텀 마커로 지정.
            customImageResourceId = R.drawable.locationpost
            isCustomImageAutoscale = false
            setCustomImageAnchor(0.5f, 1.0f)
        }
        System.out.println("돼??")
        mapView.addPOIItem(postMarker)
    }

    fun getJusoFromGeoCord(mapPoint: MapPoint?, callback: (Int?) -> Unit) {
        var getregionID: Int? = null
        mapPoint?.let {
            val currentMapPoint = MapPoint.mapPointWithGeoCoord(
                mapPoint.mapPointGeoCoord.latitude,
                mapPoint.mapPointGeoCoord.longitude
            )

            MapReverseGeoCoder(
                "50dcc7154fb1c7eb3860aa0b6c24cdc0",
                currentMapPoint,
                object : MapReverseGeoCoder.ReverseGeoCodingResultListener {
                    override fun onReverseGeoCoderFoundAddress(
                        p0: MapReverseGeoCoder?,
                        address: String
                    ) {
                        postAddress = address
                        val regionAddress: List<String> = postAddress?.split(";") ?: emptyList()

                        if (regionAddress.size >= 3) {
                            val district =
                                regionAddress[0] + " " + regionAddress[1] + " " + regionAddress[2]

                            RecordApiManager.regionRead("district", district, "Region") { success ->
                                if (success != null) {
                                    postregionID = success
                                    getregionID = success
                                    System.out.println(getregionID)
                                    System.out.println("regionID!!!")
                                } else {
                                    System.out.println("regionID..")
                                }

                                // 비동기 작업이 완료되면 콜백 호출
                                callback(getregionID)
                            }
                        } else {
                            System.out.println("잘못된 주소 형식")
                            // 비동기 작업이 실패한 경우에도 콜백 호출
                            callback(null)
                        }
                    }

                    override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
                        System.out.println("주소를 찾을 수 없습니다.")
                        // 비동기 작업이 실패한 경우에도 콜백 호출
                        callback(null)
                    }
                },
                this
            ).startFindingAddress()
        }
    }

    fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = 0xff424242.toInt()
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
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

            mapPolyline.addPoint(mapPoint)
            // 폴리라인 스타일 설정
            mapPolyline.lineColor = Color.argb(128, 0, 128, 0) // 적절한 색상 및 투명도 지정
            //mapPolyline.line= 5 // 폴리라인 두께 설정

            // 지도에 폴리라인 추가
            mapView.addPolyline(mapPolyline)

            // 폴리라인 업데이트
            //updatePathPolyline()

            // 거리 업데이트
            val now = Location("now")
            now.latitude = mapPoint.mapPointGeoCoord.latitude
            now.longitude = mapPoint.mapPointGeoCoord.longitude
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
    class DragEventListener(val mapActivity: MapActivity) : MapView.MapViewEventListener {

        override fun onMapViewInitialized(p0: MapView?) {
        }

        override fun onMapViewCenterPointMoved(p0: MapView?, movedCenter: MapPoint?) {

            if (mapActivity.isTrashButtonClicked) {
                mapActivity.binding.updateLayout.visibility = View.VISIBLE

            }

            if (mapActivity.isPostButtonClicked) {
                // 마커 변경이 클릭되어 있는 경우에만 사용해보기...
                mapActivity.postPoint = movedCenter
                // post마커 위치 변경~
                mapActivity.createpostMarker(movedCenter!!, mapActivity.mapView)
            }

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
            // 중심 좌표 주소 가져오기
            mapActivity.getJusoFromGeoCord(p1) { postregionID ->
                // 비동기 작업이 완료된 후에 실행되는 코드 블록

                System.out.println(postregionID)
                System.out.println("드래그...")
                mapActivity.binding.postAddressTextView.text = mapActivity.postAddress

                // 여기에서 postregionID를 활용하여 다른 작업을 수행할 수 있음
                // 예: postregionID를 이용하여 모달창을 띄우거나 다른 작업 수행
                // 예를 들면, mapActivity.postregionID = postregionID 와 같은 작업 수행 가능
                mapActivity.postregionID = postregionID
            }
        }

        override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
        }

    }

    // 마커 클릭 이벤트 리스너
    class MarkerEventListener(val context: Context) : MapView.POIItemEventListener {
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

        override fun onCalloutBalloonOfPOIItemTouched(
            mapView: MapView?,
            poiItem: MapPOIItem?,
            buttonType: MapPOIItem.CalloutBalloonButtonType?
        ) {
            // 말풍선 클릭 시

            val builder = AlertDialog.Builder(context)
            val itemList = arrayOf("토스트", "마커 삭제", "취소")
            builder.setTitle("test")
            builder.setItems(itemList) { dialog, which ->
                when (which) {
                    0 -> Toast.makeText(context, "토스트", Toast.LENGTH_SHORT).show()  // 토스트
                    1 -> mapView?.removePOIItem(poiItem)    // 마커 삭제
                    2 -> dialog.dismiss()   // 대화상자 닫기
                }
            }
            builder.show()
        }

        override fun onDraggablePOIItemMoved(
            mapView: MapView?,
            poiItem: MapPOIItem?,
            mapPoint: MapPoint?
        ) {
            // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
        }
    }

    // 커스텀 말풍선 클래스
    class CustomBalloonAdapter(
        val mapActivity: MapActivity,
        inflater: LayoutInflater,
        private val isTrashButtonClicked: Boolean,
        private val isToiletButtonClicked: Boolean,
        private val activity: FragmentManager
    ) : CalloutBalloonAdapter, CustomBottomSheetPostFragment.BottomSheetPostListener {
        val mCalloutBalloon: View = inflater.inflate(R.layout.balloon_layout, null)
        val name: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_name)
        val distance: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_distance)
        val total: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_total)

        override fun onBottomSheetPostDismissed(data: Boolean) {
            System.out.println(data)
            System.out.println("제보 잘 했나.")

            // 위치 제보 뷰 없애기
            mapActivity.binding.postView.visibility = View.GONE

            // 트래킹 모드 다시 시작
            mapActivity.mapView.currentLocationTrackingMode =
                MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading

            // 위치 제보 마커 없애기
            // 기존 중심 좌표 마커 삭제
            mapActivity.mapView.removePOIItem(mapActivity.postMarker)

            // 색깔 흰색으로 바꾸기 : 초기화
            mapActivity.binding.postButton.setBackgroundResource(R.drawable.circular_background)
            mapActivity.isPostButtonClicked = false

        }

        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // 마커 클릭 시 나오는 말풍선
            System.out.println(isTrashButtonClicked)
            /*if(isTrashButtonClicked || isToiletButtonClicked){
                name.text = poiItem?.itemName  // 해당 마커의 정보 이용 가능
                total.text = ""
                distance.text = ""
                return mCalloutBalloon
            }
*/
            val date: ArrayList<String> = if (poiItem?.itemName?.contains(";") == true) {
                poiItem.itemName.split(";") as ArrayList<String>
            } else {
                name.text = poiItem?.itemName  // 해당 마커의 정보 이용 가능
                total.text = ""
                distance.text = ""
                return mCalloutBalloon
            }

            if (date.size == 3) {
                System.out.println(date)
                System.out.println("마커야~~")
                name.text = date[0]   // 해당 마커의 정보 이용 가능
                total.text = "총 ${date[1]}회 방문"
                distance.text = date[2] + "km"
            } else {
                name.text = poiItem?.itemName  // 해당 마커의 정보 이용 가능
                total.text = ""
                distance.text = ""
            }



            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {

            /*if(poiItem!=null){
                val bottomSheetPostFragment = CustomBottomSheetPostFragment.newInstance(
                    poiItem.mapPoint.mapPointGeoCoord.longitude,
                    poiItem.mapPoint.mapPointGeoCoord.latitude,
                    poiItem.tag,
                    this
                )

                bottomSheetPostFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)

                // 여기서 activity를 사용하여 FragmentManager를 얻어옵니다.
                activity?.let {
                    bottomSheetPostFragment.show(it, bottomSheetPostFragment.tag)
                }
            }*/

            // 말풍선 클릭 시
            name.text = "제보하기"
            return mCalloutBalloon
        }
    }

    private fun showPauseDialog() {
        val dialog = Dialog(this, R.style.CustomDialogTheme)
        dialog.setContentView(R.layout.custom_dialog_layout)

        // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        titleTextView.text = "일시정지"
        titleTextView.gravity = Gravity.CENTER

        // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
        val infromTextView = dialog.findViewById<TextView>(R.id.dialog_inform)
        infromTextView.text = "플로깅을 잠시 멈출까요?"
        infromTextView.gravity = Gravity.CENTER

        // 취소 및 완료 버튼을 가운데로 정렬하고 녹색으로 변경
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val completeButton = dialog.findViewById<Button>(R.id.complete_button)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        completeButton.setOnClickListener {
            isRecording = false
            timerHandler.removeCallbacks(timerRunnable)
            dialog.dismiss()
            // 모양 변경
            binding.playButton.setImageResource(R.drawable.play_solid)
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    // 로그 버튼 받아오기 이슈..
    override fun onBottomSheetDismissed(data: Boolean) {
        System.out.println("기록하기 완!")
        System.out.println(data)

        if (data) {
            runOnUiThread {
                //기존 페이지로 초기화.
                binding.startButton.visibility = View.VISIBLE
                binding.startLayout.visibility = View.VISIBLE
                binding.startTextview.visibility = View.VISIBLE

                binding.playButton.visibility = View.GONE
                binding.stopButton.visibility = View.GONE

                binding.nowView.visibility = View.GONE
                binding.timerTextView.visibility = View.GONE
                binding.distanceTextView.visibility = View.GONE

                //경로랑 마커 삭제
                removeAllPolylines()
                removeAllPOIItemMarkers()
            }
            // 기존 private 플로깅 로그 불러오기
            updateMyTrailLog((application as Plogger).loginData!!.logUserID!!)

        }
    }

    private fun showStopDialog() {
        val dialog = Dialog(this, R.style.CustomDialogTheme)
        dialog.setContentView(R.layout.custom_dialog_layout)

        // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        titleTextView.text = "플로깅 완료"
        titleTextView.gravity = Gravity.CENTER

        // 다이얼로그의 제목을 가운데로 정렬하고 글꼴 변경
        val infromTextView = dialog.findViewById<TextView>(R.id.dialog_inform)
        infromTextView.text = "기록하러 가볼까요?"
        infromTextView.gravity = Gravity.CENTER

        // 취소 및 완료 버튼을 가운데로 정렬하고 녹색으로 변경
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val completeButton = dialog.findViewById<Button>(R.id.complete_button)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        completeButton.setOnClickListener {
            isRecording = false

            timerHandler.removeCallbacks(timerRunnable)

            //stop 시.
            endTime = System.currentTimeMillis()
            System.out.println("종료시간 : " + endTime.toString())
            dialog.dismiss()


            // 기록 모달창 슬라이딩 윈도우

            /*val bottomSheetView = layoutInflater.inflate(R.layout.activity_save, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetDialog.setContentView(bottomSheetView)

            bottomSheetDialog.show()*/
            val pathString = path.joinToString(";") { mapPoint ->
                "${mapPoint.mapPointGeoCoord.latitude},${mapPoint.mapPointGeoCoord.longitude}"
            }

            // 마지막 주소를 기준으로 보내기
            val recordPosition = MapPoint.mapPointWithGeoCoord(
                path[path.size - 1].mapPointGeoCoord.latitude,
                path[path.size - 1].mapPointGeoCoord.longitude
            )

            // regionID 가져오기
            getJusoFromGeoCord(recordPosition) { recordregionID ->
                System.out.println("마지막 주소 값")
                System.out.println(recordregionID)

                // 기록 페이지 모달창 부르기
//                val bottomSheetFragment = recordregionID?.let {
//                    CustomBottomSheetFragment.newInstance(pausedTime, distanceInMeters, pathString, path[path.size-1].mapPointGeoCoord.longitude, path[path.size-1].mapPointGeoCoord.latitude,
//                        it, this
//                    )
//                }

                val bottomSheetFragment = CustomBottomSheetFragment.newInstance(
                    pausedTime,
                    distanceInMeters,
                    pathString,
                    path[path.size - 1].mapPointGeoCoord.longitude,
                    path[path.size - 1].mapPointGeoCoord.latitude,
                    34102,
                    this
                )

                bottomSheetFragment?.setStyle(
                    DialogFragment.STYLE_NORMAL,
                    R.style.AppBottomSheetDialogTheme
                )
                bottomSheetFragment?.show(supportFragmentManager, bottomSheetFragment.tag)
            }


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
        dialog.setCancelable(false)
        dialog.show()
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
            startLocation.latitude = centerPoint!!.mapPointGeoCoord.latitude
            startLocation.longitude = centerPoint!!.mapPointGeoCoord.longitude

            val newDistance = newLocation.distanceTo(startLocation)

            //기준점 변경
            centerPoint = MapPoint.mapPointWithGeoCoord(newLocation.latitude, newLocation.longitude)

            distanceInMeters += newDistance
            binding.distanceTextView.text = "${String.format("%.1f", distanceInMeters.div(1000))}km"
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

    //문자->이미지 형태로 바꾸기
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        try {
            // 문자 바꾸기(POST 방식으로 값 읽고 쓰면서 값이 바뀌어서 저장됨)
            val cleanBase64 = base64String.replace("//", "")

            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            pausedTime += 1000
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
            setCustomImageAnchor(0.5f, 1.0f)
            showAnimationType = MapPOIItem.ShowAnimationType.SpringFromGround
        }

        mapView.addPOIItem(marker)
    }
}
