package com.esg.plogging.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.Gravity.*
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.R
import com.esg.plogging.databinding.ActivityMypageBinding

class MyPageActivity : AppCompatActivity() {


    lateinit var binding : ActivityMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //로그인 정보 불러오기
        val myApp = application as Plogger
        var loginData = myApp.loginData
        var dataList: ArrayList<PloggingLogData> = ArrayList()
        var totalDistance = loginData?.totalDistance?.div(1000)

        binding = ActivityMypageBinding.inflate(layoutInflater)

        if (loginData != null) {
            // 개인정보 셋팅
            binding.usernameTextView.setText(loginData.nickname)
            binding.distanceTextView.setText(String.format("%.1f km", totalDistance))

            if(!loginData.profilePhoto.equals(" "))
                binding.userImageView.setImageBitmap(decodeBase64ToBitmap(loginData.profilePhoto))

            //binding.elapsedTimeTextView.setText(loginData.totalTime.toString())
            updateTimer(loginData.totalTime as Int)

            //스탬프 정보(로그 정보) 읽어오기
            RecordApiManager.read("UserID",loginData?.logUserID, "PloggingLog") { success ->
                if (success!=null) {
                    // 기록 불러오기 성공 처리
                    dataList = success
                    runOnUiThread {
                        binding.stampCount.setText(dataList.size.toString()+"개")

                        // 개수에 맞게 스탬프 동적 생성
                        createStampViews(dataList)

                        Toast.makeText(this, "기록 불러오기 성공", Toast.LENGTH_SHORT).show()
                        // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                    }
                } else {
                    binding.stampCount.setText("0개")

                    // 기록 불러오기 실패 처리
                    runOnUiThread {
                        Toast.makeText(this, "기록 없음", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        binding.previousButton.setOnClickListener(){
            // 현재 Activity에서 다른 Activity로 전환
            val intent = Intent(applicationContext, MapActivity::class.java)
            startActivity(intent)
            // 옆으로 쓸어넘기는 듯한 효과를 추가
            //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.nextButton.setOnClickListener() {
            // 로그아웃하기
            myApp.loginData = null
            val intent = Intent(applicationContext, MapActivity::class.java)
            startActivity(intent)

            //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

        }

        setContentView(binding.root)
    }
    private fun updateTimer(elapsedTime: Int) {
        val seconds = (elapsedTime / 1000)
        val minutes = seconds / 60
        val hours = minutes / 60
        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        binding.elapsedTimeTextView.setText(formattedTime)
    }
    private fun createStampViews(stampList: List<PloggingLogData>) {
        val stampArray = arrayOf(
            R.drawable.stamp_01,
            R.drawable.stamp_02,
            R.drawable.stamp_03,
            R.drawable.stamp_04,
            R.drawable.stamp_05,
            R.drawable.stamp_06,
            R.drawable.stamp_07
        )

        val inflater = LayoutInflater.from(this)
        val stampBookLayout = binding.gridLayout

        //행, 열 개수 계산
        //stampBookLayout.rowCount=stampList.size/3+1
        //stampBookLayout.columnCount=3 // 한 줄당 3개씩

        for ((index,stamp) in stampList.withIndex()) {
            // 부모 레이아웃
            val parentLayout = LinearLayout(this)
            val params =  LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.setMargins(20,20,20,20)
            parentLayout.layoutParams = params
            parentLayout.orientation = LinearLayout.HORIZONTAL
            parentLayout.setPadding(5, 5, 5, 5)
            parentLayout.setBackgroundResource(R.drawable.rounded_user_plog_inform)

            // ImageView 설정
            val imageView = ImageView(this)
            val imageParams = LinearLayout.LayoutParams(
                200,
                200 // 또는 원하는 높이
            )

            imageParams.setMargins(20 ,20,20,20)
            imageParams.gravity = CENTER
            imageView.setPadding(20,20,20,20)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.layoutParams = imageParams
            //imageView.layoutParams.gravity = android.view.Gravity.CENTER

            imageView.adjustViewBounds = false
            imageView.elevation = 8f // 8dp로 설정

            //imageView.setImageBitmap(decodeBase64ToBitmap(stamp.TrashStroagePhotos)) // 이미지 리소스 설정
            imageView.setImageBitmap(decodeBase64ToBitmap(stamp.TrashStroagePhotos)) // 이미지 리소스 설정

            parentLayout.addView(imageView)


            val informLayout = LinearLayout(this)
            val informparams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            informparams.gravity = CENTER
            informLayout.layoutParams = informparams
            informLayout.gravity = CENTER_VERTICAL
            informLayout.orientation = LinearLayout.VERTICAL

            informLayout.setPadding(3, 3, 3, 3)



            // LocationName TextView 설정 (위치 이름)
            val locationNameTextView = TextView(this)
            val locationtextParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            locationNameTextView.layoutParams = locationtextParams

            // 폰트 설정
            locationNameTextView.typeface = resources.getFont(R.font.noto_extrabold)

            // 나머지 속성 설정
            locationNameTextView.text = stamp.locationName

            locationNameTextView.textSize = 18f // 16sp로 설정
            locationNameTextView.setTypeface(null, android.graphics.Typeface.BOLD)



            // 플로깅 날짜 TextView 설정
            val locationDateTextView = TextView(this)
            val locationDatetextParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            locationDateTextView.layoutParams = locationDatetextParams

            // 폰트 설정
            locationDateTextView.typeface = resources.getFont(R.font.noto_extrabold)

            // 나머지 속성 설정
            val date : ArrayList<String> = stamp.PloggingDate.split(" ") as ArrayList<String>
            locationDateTextView.text = date[0]
            // 텍스트 색상 설정
            locationDateTextView.setTextColor(Color.parseColor("#E2AAA9A9"))

            locationDateTextView.textSize = 16f // 16sp로 설정
            locationDateTextView.setTypeface(null, android.graphics.Typeface.BOLD)

            informLayout.addView(locationNameTextView)
            informLayout.addView(locationDateTextView)

            parentLayout.addView(informLayout)

            // 부여된 인덱스를 스탬프 뷰의 태그로 설정
            parentLayout.tag = index

            // 클릭 이벤트 리스너 설정
            parentLayout.setOnClickListener { view ->
                // 클릭된 스탬프의 인덱스를 가져옴
                val clickedIndex = view.tag as Int
                // 선택된 스탬프 객체
                val selectedStamp = stampList[clickedIndex]
                //System.out.println(selectedStamp.TrashStroagePhotos)

                // PlogActivity로 이동하는 Intent 생성
                val intent = Intent(this, PlogActivity::class.java).apply{
                    // 선택된 스탬프 객체를 인텐트에 추가
                    //putExtra("selectedStamp", selectedStamp)
                    putExtra("index",clickedIndex)

                }
                startActivity(intent)
            }
            stampBookLayout.addView(parentLayout)
        }
    }
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        try {
            // 문자 바꾸기(POST 방식으로 값 읽고 쓰면서 값이 바뀌어서 저장됨)
            val cleanBase64 = base64String.replace("\\", "")
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
