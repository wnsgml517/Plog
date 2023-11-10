package com.esg.plogging.activity

// RecordActivity.kt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.esg.plogging.databinding.ActivityRecordBinding

class RecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 여기서 기록된 정보를 가져와서 UI에 표시
        val userName = intent.getStringExtra("userName")
        val elapsedTime = intent.getLongExtra("elapsedTime", 0)
        val distance = intent.getFloatExtra("distance", 0.0f)
        val path = intent.getStringArrayExtra("path")

        // 사용자 이름 설정
        binding.userNameTextView.text = "User: $userName"

        // 실행 시간, 거리 등 설정
        binding.elapsedTimeTextView.text = "Elapsed Time: ${formatElapsedTime(elapsedTime)}"
        binding.distanceTextView.text = "Distance: ${String.format("%.2f", distance)} meters"

        // TODO: 이동 경로를 보여주는 View에 대한 설정

        // 감상평 입력 및 저장 버튼 이벤트 처리
        binding.saveButton.setOnClickListener {
            val feedback = binding.feedbackEditText.text.toString()
            // TODO: 감상평을 저장하거나 처리하는 로직 추가
            finish() // 기록 페이지를 닫음
        }
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }
}
