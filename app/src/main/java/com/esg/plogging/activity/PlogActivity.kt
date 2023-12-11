package com.esg.plogging.activity

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityPlogBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapPolyline
import net.daum.mf.map.api.MapView
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager
import net.daum.mf.map.n.api.internal.NativePolylineOverlayManager.removeAllPolylines
import java.io.Serializable

@Suppress("DEPRECATION")
class PlogActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlogBinding
    private var mapViewContainer : ViewGroup? = null

    // 경로를 그리기 위한 폴리라인 객체
    private val mapPolyline: MapPolyline = MapPolyline()
    var pathString : String? = null
    var path: MutableList<MapPoint>? = null
    private lateinit var mapView : MapView
    var dataList: ArrayList<PloggingLogData> = ArrayList()

    var selectedStamp: PloggingLogData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlogBinding.inflate(layoutInflater)
        mapView = MapView(this)

        //트래킹 모드 끄기
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff

        // 줌 레벨 바꾸기
        mapView.setZoomLevel(1, true)

        //로그인 정보 불러오기
        val myApp = application as Plogger
        val loginData = myApp.loginData



        val index: Int = intent.getIntExtra("index", -1)
        // 클릭한 플로깅 로그 객체 들고오기
        //스탬프 정보(로그 정보) 읽어오기
        RecordApiManager.read("UserID",loginData?.logUserID, "PloggingLog") { success ->
            if (success!=null) {
                // 기록 불러오기 성공 처리
                dataList = success
                runOnUiThread {

                    selectedStamp = dataList[index]

                    //이동 경로 들고와서 보여주기
                    val trailID = selectedStamp?.trailID
                    RecordApiManager.pathRead("Trail_ID", trailID, "WalkingTrail") { success ->
                        if (success != null) {
                            // 성공적으로 데이터를 받아왔을 때의 처리
                            pathString = success

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

                            runOnUiThread {
                                // 들고온 경로 폴리라인 그리기
                                updatePathPolyline()
                            }
                            System.out.println(path)
                            System.out.println("루트 들고옴!!!!")
                        } else {
                            // 통신 실패 또는 데이터 오류 처리
                            System.out.println("통신 실패")
                        }
                    }


                    val encodedImage = selectedStamp?.TrashStroagePhotos
                    // 문자열을 디코딩하여 비트맵으로 변환
                    if (!encodedImage.isNullOrEmpty()) {
                        val decodedBitmap = decodeBase64ToBitmap(encodedImage)
                        //System.out.println(encodedImage)
                        //val decodedImageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                        //val decodedBitmap = BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)
                        //val decodedBitmap: Bitmap? = StringToBitmaps(encodedImage)
                        if (decodedBitmap != null) {
                            System.out.println(decodedBitmap)
                            binding.userImageView.setImageBitmap(decodedBitmap)
                        } else {
                            // Handle decoding failure
                            Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        System.out.println("이미지 없음!!!")
                    }

                    if (selectedStamp != null) {
                        //플로깅 정보 설정
                        binding.plogDate.setText(selectedStamp?.PloggingDate)
                        binding.plogName.setText(selectedStamp?.locationName)
                        //한줄평 설정
                        binding.plogOneReview.setText(selectedStamp?.OneLineReview)
                        //거리 설정
                        binding.distanceTextView.setText(String.format("%.1f", selectedStamp?.PloggingDistance?.div(1000))+"km")
                        //소요 시간 설정
                        binding.elapsedTimeTextView.setText(formatElapsedTime(selectedStamp?.PloggingTime))
                    }

                    binding.deleteButton.setOnClickListener(){
                        if(trailID!=null &&loginData!=null) {
                            loginData.logUserID?.let { it1 ->
                                DeleteApiManager.delete("PloggingLog", "Trail_ID", trailID, it1) { success ->
                                    System.out.println(trailID)
                                    System.out.println(it1)
                                    System.out.println("삭제하러가자ㅠ")
                                    if (success) {
                                        // 삭제 성공 처리
                                        runOnUiThread {
                                            val intent = Intent(applicationContext, MyPageActivity::class.java)
                                            startActivity(intent)
                                            Toast.makeText(this, "삭제 성공", Toast.LENGTH_SHORT).show()
                                            // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                                        }
                                    } else {
                                        // 삭제 실패 처리
                                        runOnUiThread {
                                            Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    binding.previousButton.setOnClickListener(){
                        val intent = Intent(applicationContext, MyPageActivity::class.java)
                        startActivity(intent)
                    }

                    mapViewContainer = binding.mapView as ViewGroup
                    mapViewContainer!!.addView(mapView)
                    setContentView(binding.root)

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

        //val selectedStamp: PloggingLogData? =
        //    intent.intentSerializable("selectedStamp", PloggingLogData::class.java)






    }
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

    fun StringToBitmaps(image: String?): Bitmap? {
        val byteArray = image?.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
    }


    fun <T : Serializable> Intent.intentSerializable(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getSerializableExtra(key, clazz)
        } else {
            System.out.println("T야??")
            this.getSerializableExtra(key) as T?
        }
    }
    // 경로를 폴리라인으로 그리는 메서드
    // 경로를 폴리라인으로 그리는 메서드
    private fun updatePathPolyline() {

        // 경로의 각 지점을 폴리라인에 추가
        for (point in path!!) {
            mapPolyline.addPoint(point)
        }

        // 폴리라인 스타일 설정
        mapPolyline.lineColor = Color.argb(128, 0, 255, 0) // 적절한 색상 및 투명도 지정


        // 지도에 폴리라인 추가
        mapView.addPolyline(mapPolyline)


        val startPoint = path?.firstOrNull()

        // 시점 지점 마커표시
        val startMarker = MapPOIItem()
        startMarker.apply {
            itemName = "시작지점"
            mapPoint = startPoint
            selectedMarkerType = MapPOIItem.MarkerType.RedPin
        }
        mapView.addPOIItem(startMarker)

        val endPoint = path?.lastOrNull()
        // 종료 지점 마커표시
        val endMarker = MapPOIItem()
        endMarker.apply {
            itemName = "종료지점"
            mapPoint = endPoint
            selectedMarkerType = MapPOIItem.MarkerType.RedPin
        }
        mapView.addPOIItem(endMarker)

        System.out.println(startPoint)
        System.out.println(endPoint)

        // 중간 지점 계산
        startPoint?.let { sp ->
            endPoint?.let { ep ->
                val midPoint = MapPoint.mapPointWithGeoCoord(
                    (sp.mapPointGeoCoord.latitude + ep.mapPointGeoCoord.latitude) / 2,
                    (sp.mapPointGeoCoord.longitude + ep.mapPointGeoCoord.longitude) / 2
                )
                mapView.setZoomLevel(1,true)

                // 지도에 폴리라인 및 중간 지점 표시
                mapView.addPolyline(mapPolyline)
                mapView.setMapCenterPoint(midPoint, true)
            }
        }
    }
    private fun formatElapsedTime(elapsedTime: Int?): String {
        val seconds = (elapsedTime?.div(1000))?.toInt()
        val minutes = seconds?.div(60)
        val hours = minutes?.div(60)
        if (minutes != null) {
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        }
        return ""
    }
}