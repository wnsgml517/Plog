package com.esg.plogging.activity

// RecordActivity.kt

import android.R
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityRecordBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapPolyline
import net.daum.mf.map.api.MapView
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Suppress("DEPRECATION")
class RecordActivity : AppCompatActivity(),MapView.POIItemEventListener,
    MapView.MapViewEventListener {

    private lateinit var binding: ActivityRecordBinding

    private lateinit var mapView : MapView
    private var mapViewContainer : ViewGroup? = null
    var bitmap : Bitmap? = null // 이미지 비트맵 값.
    var centerPoint : MapPoint? = null
    var encodedImage : String? = null
    var path: MutableList<MapPoint>? = null

    // 경로를 그리기 위한 폴리라인 객체
    private val mapPolyline: MapPolyline = MapPolyline()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        //트래킹 모드 끄기
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff


        // 현재 사용자 정보 읽어오기
        val myApp = application as Plogger
        val loginData = myApp.loginData

        val Userid = loginData?.logUserID

        // 여기서 기록된 정보를 가져와서 UI에 표시
        val userName = intent.getStringExtra("userName")
        val elapsedTime = intent.getIntExtra("elapsedTime", 0)
        val distance = intent.getDoubleExtra("distance", 0.0)
        val pathString = intent.getStringExtra("path")


        // 스탬프 이미지
        val stampArray = arrayOf(
            com.esg.plogging.R.drawable.stamp_01,
            com.esg.plogging.R.drawable.stamp_02,
            com.esg.plogging.R.drawable.stamp_03,
            com.esg.plogging.R.drawable.stamp_04,
            com.esg.plogging.R.drawable.stamp_05,
            com.esg.plogging.R.drawable.stamp_06,
            com.esg.plogging.R.drawable.stamp_07
        )

        //선택한 스탬프 이미지
        var selectedView: ImageView? = null

        path = pathString?.split(";")?.mapNotNull { pathCoordinate ->
            val coordinates = pathCoordinate.split(",")
            if (coordinates.size == 2) {
                val latitude = coordinates[0].toDouble()
                val longitude = coordinates[1].toDouble()
                MapPoint.mapPointWithGeoCoord(latitude, longitude)
            } else {
                // Handle invalid coordinates
                null
            }
        }?.toMutableList() ?: mutableListOf()

        if(path==null) //하나도 안움직였을경우...
        {
            val coordinates = pathString?.split(",")
            if (coordinates?.size == 2) {
                val latitude = coordinates[0].toDouble()
                val longitude = coordinates[1].toDouble()
                path?.add(MapPoint.mapPointWithGeoCoord(latitude, longitude))
            } else {
                // Handle invalid coordinates
                null
            }

        }
        System.out.println(pathString+"이동경로!!!")


        // 사용자 이름 설정
        binding.userNameTextView.text = "$userName 님의 발자국"

        // 실행 시간, 거리 등 설정
        binding.elapsedTimeTextView.text = "총 소요시간\n ${formatElapsedTime(elapsedTime)}"
        binding.distanceTextView.text = "이동거리\n ${String.format("%.2f", distance)} meters"

        //
        binding.gridview.adapter = StampAdapter(this, stampArray) // 어댑터 설정
        binding.gridview.setOnItemClickListener { _, view, position, _ ->
            selectedView?.clearColorFilter() // 이전에 선택한 이미지의 색상 필터 제거
            val imageView = view as ImageView
            imageView.setColorFilter(Color.argb(150, 255, 255, 255)) // 선택한 이미지에 색상 필터 적용
            selectedView = imageView // 선택한 뷰 저장
            Toast.makeText(this, "스탬프 index: $position 선택", Toast.LENGTH_SHORT).show()
        }

        //이동 경로 표시
        updatePathPolyline()

        var predefinedLocations: ArrayList<String> = arrayListOf()

        // 저장되어 있는 플로깅 정보 기록 이름 들고오기
        RecordApiManager.read("UserID", Userid, "PloggingLog") { success ->
            if (success!=null) {
                // 불러오기 성공
                runOnUiThread {
                    // 이름만 들고와서 string배열로 처리
                    predefinedLocations = success.map { it.locationName } as ArrayList<String>
                    System.out.println(predefinedLocations)
                    System.out.println("들고옴?.")
                    // AutoCompleteTextView 초기화
                    val autoCompleteTextView: AutoCompleteTextView = binding.editTextLocation
                    val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, predefinedLocations)
                    autoCompleteTextView.setAdapter(adapter)

                    // Spinner 초기화
                    val spinner: Spinner =binding.spinnerLocations
                    val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, predefinedLocations)
                    spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = spinnerAdapter

                    // Spinner에서 항목을 선택하는 이벤트를 처리하는 리스너 설정
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                            // Spinner에서 선택한 항목 처리
                            val selectedLocation = spinner.selectedItem.toString()
                            autoCompleteTextView.setText(selectedLocation)
                            autoCompleteTextView.dismissDropDown()
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>) {
                            // 아무것도 하지 않음
                        }
                    }


                    Toast.makeText(this, "불러오기 성공", Toast.LENGTH_SHORT).show()
                    // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                }
            } else {
                // 불러오기 실패
                runOnUiThread {
                    Toast.makeText(this, "불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // 갤러리에서 이미지 선택
        binding.photoImageView.setOnClickListener {
            // 갤러리에서 이미지 선택을 위한 Intent
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 감상평 입력 및 저장 버튼 이벤트 처리
        binding.saveButton.setOnClickListener {
            System.out.println("이미지 저장 버튼 누를 때")
            System.out.println(encodedImage)
            val OneLineReview = binding.feedbackEditText.text.toString()
            val TrashStroagePhotos = encodedImage
            val locationName = binding.editTextLocation.text.toString()


            // TODO: 감상평을 저장하거나 처리하는 로직 추가
            if (pathString != null && Userid != null &&encodedImage!=null) {
                var ploggingLogData = PloggingLogData(Userid, getCurrentDateTime(),
                    locationName,distance, encodedImage!!,OneLineReview,elapsedTime,0)
                System.out.println(pathString)
                RecordApiManager.record(ploggingLogData, pathString,centerPoint?.mapPointGeoCoord!!.latitude, centerPoint?.mapPointGeoCoord!!.longitude, 34012) { success ->
                    if (success) {
                        // 가입 성공 처리
                        runOnUiThread {
                            Toast.makeText(this, "기록 성공", Toast.LENGTH_SHORT).show()
                            // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                        }
                    } else {
                        // 가입 실패 처리
                        runOnUiThread {
                            Toast.makeText(this, "기록 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            mapViewContainer?.removeAllViews();
            finish() // 기록 페이지를 닫음
        }

        mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer!!.addView(mapView)
        setContentView(binding.root)
    }


    fun getCurrentDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // 포맷을 원하는 대로 지정 가능
        return currentDateTime.format(formatter)
    }

    // 현재 사용자 위치추적

    // 경로를 폴리라인으로 그리는 메서드
    private fun updatePathPolyline() {
        NativePolylineOverlayManager.removeAllPolylines() // 기존의 폴리라인 삭제
        System.out.println(path)
        System.out.println("path")
        // 경로의 각 지점을 폴리라인에 추가
        for (point in path!!) {
            mapPolyline.addPoint(point)
        }

        // 폴리라인 스타일 설정
        mapPolyline.lineColor = Color.argb(128, 0, 255, 0) // 적절한 색상 및 투명도 지정
        // mapPolyline.= 5 // 폴리라인 두께 설정

        // 지도에 폴리라인 추가
        mapView.addPolyline(mapPolyline)


        // 중간 지점 계산
        val startPoint = path?.firstOrNull()
        val endPoint = path?.lastOrNull()

        // 로그에 저장할 위치 (종료시점)
        centerPoint = endPoint


        // 시점 지점 마커표시
        val startMarker = MapPOIItem()
        startMarker.apply {
            itemName = "시작지점"
            mapPoint = startPoint
            selectedMarkerType = MapPOIItem.MarkerType.RedPin
        }
        mapView.addPOIItem(startMarker)

        // 종료 지점 마커표시
       val endMarker = MapPOIItem()
        endMarker.apply {
            itemName = "종료지점"
            mapPoint = endPoint
            selectedMarkerType = MapPOIItem.MarkerType.RedPin
        }
        mapView.addPOIItem(endMarker)

        startPoint?.let { sp ->
            endPoint?.let { ep ->
                val midPoint = MapPoint.mapPointWithGeoCoord(
                    (sp.mapPointGeoCoord.latitude + ep.mapPointGeoCoord.latitude) / 2,
                    (sp.mapPointGeoCoord.longitude + ep.mapPointGeoCoord.longitude) / 2
                )

                // 지도에 폴리라인 및 중간 지점 표시
                mapView.addPolyline(mapPolyline)
                mapView.setMapCenterPoint(midPoint, true)
            }
        }
    }
    private fun formatElapsedTime(elapsedTime: Int): String {
        val seconds = (elapsedTime / 1000)
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }
    // onActivityResult 메서드에서 이미지 선택 후 처리를 합니다.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // 선택한 이미지의 URI를 가져와서 ImageView에 설정
            val selectedImageUri = data.data
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
            //bitmap = resize(bitmap!!)
            encodedImage = bitmapToByteArray(bitmap!!)


            // 이미지를 Base64로 인코딩하여 저장된 문자열을 디코딩하여 비트맵으로 변환
            val decodedImageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)


            System.out.println("이미지 선택할 때")
            System.out.println(encodedImage)
            binding.photoImageView.setImageBitmap(decodedBitmap)

        }
    }
    //비트맵 사이즈 변경
    private fun resize(bm: Bitmap): Bitmap? {
        var bm: Bitmap? = bm
        val config: Configuration = resources.configuration
        bm =
            if (config.smallestScreenWidthDp >= 800) Bitmap.createScaledBitmap(
                bm!!,
                400,
                240,
                true
            ) else if (config.smallestScreenWidthDp >= 600) Bitmap.createScaledBitmap(
                bm!!, 300, 180, true
            ) else if (config.smallestScreenWidthDp >= 400) Bitmap.createScaledBitmap(
                bm!!, 200, 120, true
            ) else if (config.smallestScreenWidthDp >= 360) Bitmap.createScaledBitmap(
                bm!!, 180, 108, true
            ) else Bitmap.createScaledBitmap(bm!!, 160, 96, true)
        return bm
    }
    // 기존의 Bitmap을 String 문자열(DB 저장용) 으로 바꿔주는...!
    fun bitmapToByteArray(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    /**바이너리 바이트 배열을 스트링으로 바꾸어주는 메서드  */
    fun byteArrayToBinaryString(b: ByteArray): String? {
        val sb = StringBuilder()
        for (i in b.indices) {
            sb.append(byteToBinaryString(b[i]))
        }
        return sb.toString()
    }

    /**바이너리 바이트를 스트링으로 바꾸어주는 메서드  */
    fun byteToBinaryString(n: Byte): String? {
        val sb = java.lang.StringBuilder("00000000")
        for (bit in 0..7) {
            if ((n.toInt() shr bit) and 1 > 0) {
                sb.setCharAt(7 - bit, '1')
            }
        }
        return sb.toString()
    }
    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
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
