package com.esg.plogging.activity

// LoginActivity.kt

import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityLoginBinding
import com.esg.plogging.databinding.ActivityMapBinding

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
            finish()
        }
        setContentView(binding.root)
    }
}
