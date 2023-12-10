package com.esg.plogging.activity

import android.graphics.Paint.Join
import android.os.Bundle
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


class JoinActivity : AppCompatActivity() {
    lateinit var binding : ActivityJoinBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityJoinBinding.inflate(layoutInflater)

        binding.goJoinButton.setOnClickListener {
            attemptJoin()
        }


        setContentView(binding.root)
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
            JoinStart(Nickname, UserID, Passwd)
            //startJoin(JoinData(name, email, password))
        }
    }
    private fun JoinStart(nickname: String, userID: String, passwd: String) {
        JoinApiManager.join(nickname, userID, passwd) { success ->
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

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

}