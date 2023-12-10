package com.esg.plogging.activity

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// JoinApiManager.kt
class TrashApiManager {
    companion object {
        fun trashLocationGet(value: String, key: String, Table : String, callback: ( ArrayList<TrashLocationData>?) -> Unit) {
            Thread {
                try {
                    val serverAddress = "http://13.209.47.199/readData.php";
                    val url = URL(serverAddress)

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false


                    val data = "key=" + key + "&Table=" + Table+"&value="+value;

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
                    val jsonObject = JSONObject(jsonStr)
                    System.out.println(jsonObject.getString("0"))
// 'column_name' 키의 값을 무시하고 나머지 키들을 배열로 가져옴

                    val dataList: ArrayList<TrashLocationData> = ArrayList()

                    val keys = jsonObject.keys()

                    keys.next()

                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        val data = jsonObject.getString(key)
                        val dataitem = JSONObject(data)
                        //for문.... column name 전처리...
                        // 이제 dataObject에서 필요한 데이터를 추출하고 TrashLocationData 객체를 생성하여 dataList에 추가
                        val trashId = dataitem.getString("TrashCanID")
                        val latitude = dataitem.getString("latitude").toDouble()
                        val longitude = dataitem.getString("longitude").toDouble()
                        val trashName = dataitem.getString("locationName")

                        val trashLocationData = TrashLocationData(trashId, trashName, latitude, longitude)
                        dataList.add(trashLocationData)
                    }


                    // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                    callback(dataList)


                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
    }
}
