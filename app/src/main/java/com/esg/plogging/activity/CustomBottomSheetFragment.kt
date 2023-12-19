package com.esg.plogging.activity

import android.app.Activity
import android.app.AlertDialog
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
import com.esg.plogging.databinding.ActivityMapBinding
import com.esg.plogging.databinding.ActivitySaveBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CustomBottomSheetFragment : BottomSheetDialogFragment() {

    interface BottomSheetListener {
        fun onBottomSheetDismissed(data: Boolean)
    }
    private var bottomSheetListener: BottomSheetListener? = null

    private lateinit var binding: ActivitySaveBinding
    private lateinit var mbinding: ActivityMapBinding
    var encodedImage : String? = null
    var bitmap : Bitmap? = null // 이미지 비트맵 값.
    private val REQUEST_IMAGE_CAPTURE = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivitySaveBinding.inflate(inflater, container, false)
        mbinding = ActivityMapBinding.inflate(inflater, container, false)

        // 플로깅 정보 들고오기
        val elapsedTime = arguments?.getInt("elapsedTime")
        val distance = arguments?.getDouble("distance")?.div(1000)
        val path = arguments?.getString("path")
        val latitude = arguments?.getDouble("latitude")
        val longitude = arguments?.getDouble("longitude")
        val regionID = arguments?.getInt("regionID")

        System.out.println("위경도")
        System.out.println(longitude)
        System.out.println(latitude)
        System.out.println(regionID)


        //로그인 정보
        val myApp = activity?.application as Plogger
        val loginData = myApp.loginData

        // 실행 시간, 거리 정보 설정, 오늘 날짜 설정
        binding.elapsedTimeTextView.text = "${elapsedTime?.let { formatElapsedTime(it) }}"
        binding.distanceTextView2.text = "${String.format("%.1f", distance)}km"
        binding.dayView.text = getCurrentDay()


        var predefinedLocations: ArrayList<String> = arrayListOf()

        // 저장되어 있는 플로깅 정보 기록 이름 들고오기
        RecordApiManager.privateTrailRead("UserID", loginData?.logUserID, "PrivateTrail") { success ->
            if (success!=null) {
                // 불러오기 성공
                activity?.runOnUiThread {
                    // 이름만 들고와서 string배열로 처리
                    predefinedLocations = success.map { it.locationName } as ArrayList<String>
                    System.out.println(predefinedLocations)
                    System.out.println("들고옴?.")
                    // AutoCompleteTextView 초기화
                    val autoCompleteTextView: AutoCompleteTextView = binding.editTextLocation
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, predefinedLocations)
                    autoCompleteTextView.setAdapter(adapter)

                    // Spinner 초기화
                    val spinner: Spinner =binding.spinnerLocations
                    val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, predefinedLocations)
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

                    System.out.println("불러오기 성공")
                }
            } else {
                // 불러오기 실패
                activity?.runOnUiThread {
                   System.out.println("불러오기 실패")
                }
            }
        }


        // 갤러리에서 이미지 선택
        binding.photoImageView.setOnClickListener {
            // 갤러리에서 이미지 선택을 위한 Intent

            showOptionDialog()

            //val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            //startActivityForResult(intent, CustomBottomSheetFragment.PICK_IMAGE_REQUEST)
        }

        binding.saveButton.setOnClickListener() {
            System.out.println(encodedImage)
            val OneLineReview = binding.feedbackEditText.text.toString()
            val TrashStroagePhotos = encodedImage
            val locationName = binding.editTextLocation.text.toString()

            // 저장이 성공했을 때 메인 액티비티로 값을 전달
            bottomSheetListener?.onBottomSheetDismissed(true)



            // TODO: 감상평을 저장하거나 처리하는 로직 추가
            if (path != null && distance != null && loginData?.logUserID != null && encodedImage != null
                && longitude != null && latitude != null && elapsedTime != null) {
                var ploggingLogData = PloggingLogData(loginData?.logUserID, getCurrentDateTime(),
                    locationName,distance, encodedImage!!,OneLineReview,elapsedTime,0)

                // 로그 기록
                RecordApiManager.record(
                    ploggingLogData,
                    path,
                    latitude,
                    longitude,
                    regionID!!
                ) { success ->
                    if (success) {
                        System.out.println("기록 성공")
                        // 가입 성공 처리
                        activity?.runOnUiThread {

                                                    }
                    } else {
                        System.out.println("기록 실패")
                        // 가입 실패 처리
                        activity?.runOnUiThread {
                            System.out.println("기록 실패")
                        }
                    }
                }
            }


            dismiss() // 기록 창 닫기
        }

        val view = inflater.inflate(R.layout.activity_save, container, false)

        return binding.root
    }
    private fun formatElapsedTime(elapsedTime: Int): String {
        val seconds = (elapsedTime / 1000)
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }
    fun getCurrentDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // 포맷을 원하는 대로 지정 가능
        return currentDateTime.format(formatter)
    }
    fun getCurrentDay(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 포맷을 원하는 대로 지정 가능
        return currentDateTime.format(formatter)
    }
    //사용자 비트맵을 string으로 변환하는 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> handleGalleryResult(data)
                REQUEST_IMAGE_CAPTURE -> handleCameraResult(data)
            }
        }
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {    // 선택한 이미지의 URI를 가져와서 ImageView에 설정
//            val selectedImageUri = data.data
//            val inputStream =  requireActivity().contentResolver.openInputStream(selectedImageUri!!)
//            bitmap = BitmapFactory.decodeStream(inputStream)
//            //bitmap = resize(bitmap!!)
//            encodedImage = bitmapToByteArray(bitmap!!)
//
//
//            // 이미지를 Base64로 인코딩하여 저장된 문자열을 디코딩하여 비트맵으로 변환
//            val decodedImageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
//            val decodedBitmap = BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)
//
//
//            System.out.println("이미지 선택할 때")
//            System.out.println(encodedImage)
//            binding.photoImageView.setImageBitmap(decodedBitmap)
//
//        }
    }
    // 기존의 Bitmap을 String 문자열(DB 저장용) 으로 바꿔주는...!
    fun bitmapToByteArray(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    private fun showOptionDialog() {
        val options = arrayOf("갤러리에서 선택", "카메라로 촬영")

        AlertDialog.Builder(requireContext())
            .setTitle("이미지 선택 옵션")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun handleGalleryResult(data: Intent?) {
        val selectedImageUri = data?.data
        val inputStream =  requireActivity().contentResolver.openInputStream(selectedImageUri!!)
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

    private fun handleCameraResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as Bitmap
        //bitmap = resize(bitmap!!)
        encodedImage = bitmapToByteArray(imageBitmap!!)


        // 이미지를 Base64로 인코딩하여 저장된 문자열을 디코딩하여 비트맵으로 변환
        val decodedImageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)


        System.out.println("이미지 선택할 때")
        System.out.println(encodedImage)
        binding.photoImageView.setImageBitmap(decodedBitmap)
    }

    companion object {

        private const val PICK_IMAGE_REQUEST = 1

        fun newInstance(elapsedTime: Int, distance: Double, path: String, longitude : Double, latitude : Double, regionID : Int, listener: BottomSheetListener): CustomBottomSheetFragment {
            val fragment = CustomBottomSheetFragment()
            val args = Bundle()

            fragment.bottomSheetListener = listener
            args.putInt("elapsedTime", elapsedTime)
            args.putDouble("distance", distance)
            args.putDouble("longitude", longitude)
            args.putDouble("latitude", latitude)
            args.putInt("regionID", regionID)
            args.putString("path", path)


            fragment.arguments = args
            return fragment
        }
    }

}