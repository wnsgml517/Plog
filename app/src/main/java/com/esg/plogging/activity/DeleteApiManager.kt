package com.esg.plogging.activity

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// JoinApiManager.kt
class DeleteApiManager {
    companion object {
        fun delete(table: String, key: String, value: Int ,userID : String, callback: (DeleteResponse?) -> Unit) {
            Thread {
                try {
                    System.out.println("삭제...........")
                    val serverAddress = "http://13.209.47.199/deleteData.php"
                    val url = URL(serverAddress)

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false

                    val data = "table=$table&key=${key}&value=$value&UserID=$userID"

                    val os: OutputStream = connection.outputStream
                    val writer = OutputStreamWriter(os)
                    writer.write(data, 0, data.length)
                    writer.flush()
                    writer.close()


                    System.out.println("삭제...........")

                    val `is`: InputStream = connection.inputStream
                    val isr = InputStreamReader(`is`)
                    val reader = BufferedReader(isr)
                    val buffer = StringBuffer()
                    while (true) {
                        val line: String = reader.readLine() ?: break
                        buffer.append("$line\n")
                    }
                    System.out.println(buffer.toString())
                    System.out.println("삭제~~")

                    val jsonStr = buffer.toString()
                    val jsonObject = JSONObject(jsonStr)

                    // 서버 응답 확인
                    val TotalLog = jsonObject.getString("TotalLog")
                    val TotalDistance = jsonObject.getString("TotalDistance").toDouble()
                    val TotalTime = jsonObject.getString("TotalTime").toInt()

                    val serverResponse = DeleteResponse(TotalLog, TotalDistance, TotalTime)

                    callback(serverResponse)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
    }
}
