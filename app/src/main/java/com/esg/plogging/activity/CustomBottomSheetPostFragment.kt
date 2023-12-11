package com.esg.plogging.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.R
import com.esg.plogging.databinding.ActivityPostBinding
import com.esg.plogging.databinding.ActivitySaveBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CustomBottomSheetPostFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityPostBinding
    var encodedImage : String? = null
    var bitmap : Bitmap? = null // 이미지 비트맵 값.
    private var separator: Int = 0 // 초기값은 0으로 설정
    var regionID : Int  = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityPostBinding.inflate(inflater, container, false)
        // 위치 정보 들고오기
        val latitude = arguments?.getDouble("latitude")
        val longitude = arguments?.getDouble("longitude")
        val regionID = arguments?.getInt("regionID")

        //로그인 정보
        val myApp = activity?.application as Plogger
        val loginData = myApp.loginData

        // 클릭한 제보 종류
        binding.trashPostView.setOnClickListener(){
            separator = 0
            // Color.parseColor("#D6D98B")를 사용하여 색상 문자열을 Color 객체로 변환
            val color = android.graphics.Color.parseColor("#D6D98B")

            // ColorStateList 생성
            val colorStateList = android.content.res.ColorStateList.valueOf(color)

            // backgroundTintList 속성 설정
            binding.trashPostView.backgroundTintList = colorStateList
            // backgroundTintList 속성 설정
            binding.toiletPostView.backgroundTintList = null

        }
        // 클릭한 제보 종류
        binding.toiletPostView.setOnClickListener(){
            separator = 0
            // Color.parseColor("#D6D98B")를 사용하여 색상 문자열을 Color 객체로 변환
            val color = android.graphics.Color.parseColor("#D6D98B")

            // ColorStateList 생성
            val colorStateList = android.content.res.ColorStateList.valueOf(color)

            // backgroundTintList 속성 설정
            binding.toiletPostView.backgroundTintList = colorStateList

            // backgroundTintList 속성 설정
            binding.trashPostView.backgroundTintList = null

        }

        // 저장하기 제보
        binding.saveButton.setOnClickListener() {


            if (binding.editTextLocation.text != null
                && loginData?.logUserID != null
                && regionID != null
                && longitude != null
                && latitude != null) {

                RecordApiManager.locationPost(
                    loginData.logUserID,
                    regionID,
                    binding.editTextLocation.text.toString(),
                    longitude,
                    latitude,
                    separator
                ) { success ->
                    if (success) {
                        // 가입 성공 처리
                        activity?.runOnUiThread {
                            System.out.println("기록 성공")
                        }
                    } else {
                        // 가입 실패 처리
                        activity?.runOnUiThread {
                            System.out.println("기록 실패")
                        }
                    }
                }
            }
            dismiss() // 기록 창 닫기
        }

        val view = inflater.inflate(R.layout.activity_post, container, false)

        return binding.root
    }
    companion object {

        private const val PICK_IMAGE_REQUEST = 1

        fun newInstance(longitude : Double, latitude : Double, regionID : Int): CustomBottomSheetPostFragment {
            val fragment = CustomBottomSheetPostFragment()
            val args = Bundle()
            args.putDouble("longitude", longitude)
            args.putDouble("latitude", latitude)
            args.putInt("regionID", regionID)
            fragment.arguments = args
            return fragment
        }
    }

}