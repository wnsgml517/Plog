package com.esg.plogging.activity

import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// JoinApiManager.kt
class JoinApiManager {
    companion object {
        fun join(nickname: String, userID: String, passwd: String, callback: (Boolean) -> Unit) {
            Thread {
                try {
                    val serverAddress = "http://15.164.163.61/signup.php"
                    val url = URL(serverAddress)

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false

                    val data = "NickName=$nickname&UserID=$userID&Passwd=$passwd"

                    val os: OutputStream = connection.outputStream
                    val writer = OutputStreamWriter(os)
                    writer.write(data, 0, data.length)
                    writer.flush()
                    writer.close()

                    val `is`: InputStream = connection.inputStream
                    val isr = InputStreamReader(`is`)
                    val reader = BufferedReader(isr)
                    val buffer = StringBuffer()
                    while (true) {
                        val line: String = reader.readLine() ?: break
                        buffer.append("$line\n")
                    }

                    // 서버 응답 확인
                    val success = buffer.toString().contains("Success")
                    callback(success)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false)
                }
            }.start()
        }
    }
}
