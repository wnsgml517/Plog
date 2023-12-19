package com.esg.plogging.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityEditBinding
import java.io.*


@Suppress("DEPRECATION")
class EditActivity : AppCompatActivity() {
    lateinit var binding : ActivityEditBinding
    var bitmap : Bitmap? = null // 이미지 비트맵 값.
    var encodedImage : String? = null // 이미지 String 값
    var myApp : Plogger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBinding.inflate(layoutInflater)

        myApp = application as Plogger
        val loginData = myApp!!.loginData

        encodedImage = loginData!!.profilePhoto

        // 초기값 셋팅
        val Nickname = intent.getStringExtra("NickName")
        val Bio = intent.getStringExtra("Bio")
        val Photo = intent.getStringExtra("Photo")

        binding.NicknameEditText.setText(loginData!!.nickname)
        binding.bioEditText.setText(loginData!!.bio)
        binding.userImageView.setImageBitmap(decodeBase64ToBitmap(loginData!!.profilePhoto))

        binding.goEditButton.setOnClickListener {
            attemptEdit()

        }

        // 갤러리에서 이미지 선택
        binding.userImageView.setOnClickListener {
            // 갤러리에서 이미지 선택을 위한 Intent
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, JoinActivity.PICK_IMAGE_REQUEST)
        }


        setContentView(binding.root)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EditActivity.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // 선택한 이미지의 URI를 가져와서 ImageView에 설정
            val selectedImageUri = data.data
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
            binding.userImageView.setImageBitmap(bitmap)
            //bitmap = resize(bitmap!!)
            encodedImage = bitmapToByteArray(bitmap!!)

            /*
            // 이미지를 Base64로 인코딩하여 저장된 문자열을 디코딩하여 비트맵으로 변환
            val decodedImageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)


            System.out.println("이미지 선택할 때")
            System.out.println(encodedImage)
            binding.photoImageView.setImageBitmap(decodedBitmap)
             */

        }
    }
    // 기존의 Bitmap을 String 문자열(DB 저장용) 으로 바꿔주는...!
    fun bitmapToByteArray(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    private fun attemptEdit() {
        val Nickname = binding.NicknameEditText!!.text.toString()
        val Bio = binding.bioEditText!!.text.toString()
        val Passwd = binding.pwEditText!!.text.toString()
        var cancel = false
        var focusView: View? = null

        // 패스워드의 유효성 검사
        if (Passwd.isEmpty()) {
            binding.pwEditText!!.error = "비밀번호를 입력해주세요."
            focusView = binding.pwEditText
            cancel = true
        } else if (!isPasswordValid(Passwd)) {
            binding.pwEditText!!.error = "6자 이상의 비밀번호를 입력해주세요."
            focusView = binding.pwEditText
            cancel = true
        }

        // 이메일의 유효성 검사
        if (Bio.isEmpty()) {
            binding.bioEditText!!.error = "바이오를 입력해주세요"
            focusView = binding.bioEditText
            cancel = true
        }
        // 이름의 유효성 검사
        if (Nickname.isEmpty()) {
            binding.NicknameEditText!!.error = "닉네임을 입력해주세요."
            focusView = binding.NicknameEditText
            cancel = true
        }
        if (cancel) {
            focusView?.requestFocus()
        } else {
            System.out.println("정보수정...?")
            encodedImage?.let { EditStart(myApp!!.loginData!!.logUserID!!, Nickname, Bio, Passwd, it) }
            //startJoin(JoinData(name, email, password))
        }
    }
    private fun EditStart(userID : String, nickname: String, bio: String, password: String, profile : String) {
        EditApiManager.edit(userID, nickname, bio, password, profile) { success ->
            if (success) {
                // 가입 성공 처리
                runOnUiThread {
                    val intent = Intent(this, MyPageActivity::class.java).apply{
                        // 선택된 스탬프 객체를 인텐트에 추가
                        myApp!!.loginData!!.nickname=nickname
                        myApp!!.loginData!!.bio=bio
                        myApp!!.loginData!!.profilePhoto=profile
                    }
                    startActivity(intent)

                    Toast.makeText(this, "회원정보 수정 성공", Toast.LENGTH_SHORT).show()
                    //finish() // 화면 닫기
                    // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                }
            } else {
                // 가입 실패 처리
                runOnUiThread {
                    finish() // 화면 닫기
                    Toast.makeText(this, "회원정보 수정 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }


    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
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