package com.esg.plogging.activity

// LoginActivity.kt

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {


    lateinit var binding : ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityLoginBinding.inflate(layoutInflater)

        binding.idInputLayout.setEndIconOnClickListener {
            // ID 입력창 클릭 시 처리
            // 여기에 작성하세요.
            // 예를 들면, ID 입력창 포커스 주기 등의 작업을 수행할 수 있습니다.

        }

        binding.pwInputLayout.setEndIconOnClickListener {
            // Password 입력창 클릭 시 처리
            // 여기에 작성하세요.
        }

        // 플로깅 하러가기 버튼을 누를 때 로그인 로직 수행
        binding.goPloggingButton.setOnClickListener {
            // TODO: 로그인 로직 구현

            attemptLogin();


        }
        // 회원가입 버튼 누를 때
        binding.signupTextView.setOnClickListener {
            val intent = Intent(applicationContext, JoinActivity::class.java)
            startActivity(intent)
        }

        setContentView(binding.root)
    }
    private fun attemptLogin() {
        binding.idEditText.error = null
        binding.pwEditText.error = null

        val email = binding.idEditText.text.toString()
        val password = binding.pwEditText.text.toString()

        var cancel = false
        var focusView: View? = null

        // Password validation
        if (password.isEmpty()) {
            binding.idEditText.error = "비밀번호를 입력해주세요."
            focusView = binding.idEditText
            cancel = true
        } else if (!isPasswordValid(password)) {
            binding.pwEditText.error = "6자 이상의 비밀번호를 입력해주세요."
            focusView = binding.pwEditText
            cancel = true
        }

        // Email validation
        if (email.isEmpty()) {
            binding.idEditText.error = "이메일을 입력해주세요."
            focusView = binding.idEditText
            cancel = true
        } else if (!isEmailValid(email)) {
            binding.idEditText.error = "@를 포함한 유효한 이메일을 입력해주세요."
            focusView = binding.idEditText
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            //startLogin(LoginData(email, password))
            LoginStart(email, password)

            showProgress(true)
        }
    }
    private fun LoginStart(email: String, password:String){
        LoginApiManager.login(email, password) { loginData ->
            if (loginData != null) {
                // 성공적으로 데이터를 받아왔을 때의 처리
                val myApp = application as Plogger
                myApp.loginData = loginData

                System.out.println("로그인 성공!!!!")
                val intent = Intent(applicationContext, MapActivity::class.java)
                startActivity(intent)
            } else {
                // 통신 실패 또는 데이터 오류 처리
                System.out.println("통신 실패")
            }
        }
    }
    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    private fun showProgress(show: Boolean) {
        binding.idInputLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

}
