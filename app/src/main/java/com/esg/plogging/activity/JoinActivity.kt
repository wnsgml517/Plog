package com.esg.plogging.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint.Join
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityJoinBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


@Suppress("DEPRECATION")
class JoinActivity : AppCompatActivity() {
    lateinit var binding : ActivityJoinBinding
    var bitmap : Bitmap? = null // 이미지 비트맵 값.
    var encodedImage : String? = null // 이미지 String 값


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityJoinBinding.inflate(layoutInflater)

        binding.goJoinButton.setOnClickListener {
            attemptJoin()
        }

        // 갤러리에서 이미지 선택
        binding.photoImageView.setOnClickListener {
            // 갤러리에서 이미지 선택을 위한 Intent
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, JoinActivity.PICK_IMAGE_REQUEST)
        }


        setContentView(binding.root)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JoinActivity.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // 선택한 이미지의 URI를 가져와서 ImageView에 설정
            val selectedImageUri = data.data
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
            binding.photoImageView.setImageBitmap(bitmap)
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
    private fun attemptJoin() {
        val Nickname = binding.NicknameEditText!!.text.toString()
        val UserID = binding.idEditText!!.text.toString()
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
        if (UserID.isEmpty()) {
            binding.idEditText!!.error = "이메일을 입력해주세요."
            focusView = binding.idEditText
            cancel = true
        } else if (!isEmailValid(UserID)) {
            binding.idEditText!!.error = "@를 포함한 유효한 이메일을 입력해주세요."
            focusView = binding.idEditText
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
            encodedImage?.let { JoinStart(Nickname, UserID, Passwd, it) }
            //startJoin(JoinData(name, email, password))
        }
    }
    private fun JoinStart(nickname: String, userID: String, passwd: String, profile : String) {
        JoinApiManager.join(nickname, userID, passwd, profile) { success ->
            if (success) {
                // 가입 성공 처리
                runOnUiThread {
                    Toast.makeText(this, "가입 성공", Toast.LENGTH_SHORT).show()
                    // 원하는 다음 화면으로 이동하거나 추가적인 작업 수행
                }
            } else {
                // 가입 실패 처리
                runOnUiThread {
                    Toast.makeText(this, "가입 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

}