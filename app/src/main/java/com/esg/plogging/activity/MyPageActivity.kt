package com.esg.plogging.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityLoginBinding
import com.esg.plogging.databinding.ActivityMypageBinding

class MyPageActivity : AppCompatActivity() {


    lateinit var binding : ActivityMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMypageBinding.inflate(layoutInflater)

        binding.nextButton.setOnClickListener() {
            finish()

        }

        setContentView(binding.root)
    }
}
