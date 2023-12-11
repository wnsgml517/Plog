package com.esg.plogging.activity

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class LoginApiManager {
    companion object {
        fun login(email: String, password: String, callback: (LoginData?) -> Unit) {
            Thread {
                try {
                    //1.서버로 보낼 데이터 부르기
                    val UserID = email
                    val Passwd = password

                    //2.Post방식으로 데이터 보낼 서버 주소 준비
                    val serverAdress = "http://13.209.47.199/login.php"

                    //3.서버와 통신
                    //3-1. 해임달(URL) 준비

                    val url = URL(serverAdress)

                    //3-2. Http통신을 객체를 만들기 & 4가지 set
                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.setRequestMethod("POST")
                    connection.setDoInput(true)
                    connection.setDoOutput(true)
                    connection.setUseCaches(false)

                    //3-3. 보낸 데이터를 POST방식으로 쓰기 위해 [key=value]규칙에 맞게 하나의 문자열로 결합
                    val data = "UserID=${UserID}" + "&Passwd=${Passwd}"

                    //4. 데이터를 아웃풋 스트림을 이용해서 직접 내보내기
                    val os: OutputStream = connection.getOutputStream()
                    val writer = OutputStreamWriter(os) //
                    writer.write(data, 0, data.length) //1024를 넘지 않는 사이즈로 만드는 게 좋다
                    writer.flush()
                    writer.close()

                    //5. 서버(postText.php)에서 에코한 응답문자열 읽어오기
                    val `is`: InputStream = connection.getInputStream()
                    val isr = InputStreamReader(`is`)
                    val reader = BufferedReader(isr)
                    val buffer = StringBuffer()
                    while (true) {
                        val line: String = reader.readLine() ?: break
                        buffer.append(
                            """
                        $line
                        
                        """.trimIndent()
                        )
                    }

                    val jsonStr = buffer.toString()
                    System.out.println(jsonStr)
                    val jo = JSONObject(jsonStr)
                    val logUserID = jo.getString("UserID")
                    val nickname = jo.getString("NickName")
                    val Bio = jo.getString("Bio")
                    val TotalLog = jo.getInt("TotalLog")
                    val TotalDistance = jo.getDouble("TotalDistance")
                    val TotalTime = jo.get("TotalTime")
                    //val profilePhoto = jo.getString("ProfilePhoto")
                    val profilePhoto =" "

                    //로그인 데이터 저장
                    val loginData = LoginData(logUserID, nickname, Bio, TotalLog, TotalDistance,TotalTime, profilePhoto)
                    callback(loginData)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
    }
}