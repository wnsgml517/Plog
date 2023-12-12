package com.esg.plogging.activity

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// JoinApiManager.kt
class RecordApiManager {
    companion object {
        fun record(ploggingLogData: PloggingLogData, Route : String,
                   latitude : Double, longitude : Double, RegionID : Int , callback: (Boolean) -> Unit) {
            Thread {
                try {
                    val serverAddress = "http://13.209.47.199/insertLog.php"
                    val url = URL(serverAddress)

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false


                    val data = "UserID=${ploggingLogData.UserID}"+
                            "&Route=${Route}"+
                            "&PloggingDate=${ploggingLogData.PloggingDate}" +
                            "&locationName=${ploggingLogData.locationName}"+
                            "&PloggingDistance=${ploggingLogData.PloggingDistance}"+
                            "&TrashStoragePhotos=${ploggingLogData.TrashStroagePhotos}"+
                            "&OneLineReview=${ploggingLogData.OneLineReview}"+
                            "&PloggingTime=${ploggingLogData.PloggingTime}"+
                            "&latitude=${latitude}"+
                            "&longitude=${longitude}"+
                            "&RegionID=${RegionID}"

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
                    System.out.println(buffer.toString())
                    System.out.println("플로깅정보기록~~")
                    // 서버 응답 확인
                    val success = buffer.toString().contains("Success")
                    callback(success)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false)
                }
            }.start()
        }

        fun locationPost(userid: String,
                         RegionID : Int ,
                         locationname : String,
                         latitude : Double,
                         longitude : Double,
                         separator : Int,
                         callback: (Boolean) -> Unit) {
            Thread {
                try {
                    val serverAddress = "http://13.209.47.199/insertData.php"
                    val url = URL(serverAddress)

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false


                    val data = "UserID=${userid}"+
                            "&RegionID=${RegionID}"+
                            "&locationName=${locationname}"+
                            "&latitude=${latitude}"+
                            "&longitude=${longitude}"+
                            "&Separator=${separator}"

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
                    System.out.println(buffer.toString())
                    System.out.println("위치제보~~")
                    // 서버 응답 확인
                    val success = buffer.toString().contains("Success")
                    callback(success)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false)
                }
            }.start()
        }

        fun read(key : String, value : String?, Table : String, callback: ( ArrayList<PloggingLogData>?) -> Unit){
            Thread {
                try {
                    System.out.println("읽는 중. . . . ")
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

                    System.out.println("읽는 중. . . . ")
                    System.out.println(jsonStr)

                    val dataList: ArrayList<PloggingLogData> = ArrayList()

                    val keys = jsonObject.keys()

                    keys.next()

                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        val data = jsonObject.getString(key)
                        val dataitem = JSONObject(data)

                        // 이제 dataObject에서 필요한 데이터를 추출하고 TrashLocationData 객체를 생성하여 dataList에 추가
                        val UserID = dataitem.getString("UserID")
                        val PloggingDate = dataitem.getString("PloggingDate")
                        val locationName = dataitem.getString("locationName")
                        val PloggingDistance = dataitem.getString("PloggingDistance").toDouble()
                        val TrashStoragePhotos = dataitem.getString("TrashStoragePhotos")
                        val OneLineReview = dataitem.getString("OneLineReview")
                        val PloggingTime = dataitem.getString("PloggingTime").toInt()
                        val trailID = dataitem.getString("Trail_ID").toInt()
                        //val PloggingSticker = dataitem.getString("PloggingSticker").toInt()


                        val ploggingLogData = PloggingLogData(UserID, PloggingDate,
                            locationName, PloggingDistance, TrashStoragePhotos, OneLineReview,PloggingTime,trailID)
                        dataList.add(ploggingLogData)
                    }


                    // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                    callback(dataList)


                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
        fun pathRead(key : String, value : Int?, Table : String, callback: (String?) -> Unit){
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
                    System.out.println("루트루트")
                    System.out.println(jsonStr)

                    val jsonObject = JSONObject(jsonStr)
                    val keys = jsonObject.keys()
                    keys.next()

                    val key = keys.next() as String
                    val route = jsonObject.getString(key)
                    val dataitem = JSONObject(route)
                    val Route  = dataitem.getString("Route")


                    // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                    callback(Route)


                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
        fun regionRead(key : String, value : String?, Table : String, callback: (Int?) -> Unit){
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
                    System.out.println("regionID")
                    System.out.println(jsonStr)

                    val jsonObject = JSONObject(jsonStr)
                    val keys = jsonObject.keys()
                    keys.next()

                    val key = keys.next() as String
                    val route = jsonObject.getString(key)
                    val dataitem = JSONObject(route)
                    val regionID  = dataitem.getString("Region_Code").toInt()


                    // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                    callback(regionID)


                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
        fun jsonRead(key : String, value : String?, Table : String, callback: (JSONObject?) -> Unit) {
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
                    callback(jsonObject)
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }.start()
        }
        fun privateTrailRead(key : String, value : String?, Table : String, callback: ( ArrayList<PrivateTrailData>?) -> Unit){
            jsonRead(key, value, Table) { jsonObject ->
                try {
                   if(jsonObject!=null)
                   {
                       val dataList: ArrayList<PrivateTrailData> = ArrayList()

                       val keys = jsonObject.keys()

                       keys.next()

                       while (keys.hasNext()) {
                           val key = keys.next() as String
                           val data = jsonObject.getString(key)
                           val dataitem = JSONObject(data)

                           val locationName = dataitem.getString("locationName")
                           val RegionID = dataitem.getString("RegionID").toInt()
                           val latitude = dataitem.getString("latitude").toDouble()
                           val longitude = dataitem.getString("longitude").toDouble()
                           val UserID = dataitem.getString("UserID")

                           val privateTrailData = PrivateTrailData(locationName, RegionID,
                               latitude, longitude, UserID)
                           dataList.add(privateTrailData)
                           // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                           callback(dataList)
                       }

                   }

                } catch (e: Exception) {
                    e.printStackTrace()
                    // 오류 처리
                    callback(null)
                }
            }

        }
        fun ploggingLogRead(key : String, value : String?, Table : String, callback: ( ArrayList<PloggingLogData>?) -> Unit){
            jsonRead(key, value, Table) { jsonObject ->
                try {
                    if(jsonObject!=null)
                    {
                        val dataList: ArrayList<PloggingLogData> = ArrayList()

                        val keys = jsonObject.keys()

                        keys.next()

                        while (keys.hasNext()) {
                            // 이제 dataObject에서 필요한 데이터를 추출하고 TrashLocationData 객체를 생성하여 dataList에 추가
                            val UserID = jsonObject.getString("UserID")
                            val PloggingDate = jsonObject.getString("PloggingDate")
                            val locationName = jsonObject.getString("locationName")
                            val PloggingDistance = jsonObject.getString("PloggingDistance").toDouble()
                            val TrashStoragePhotos = jsonObject.getString("TrashStoragePhotos")
                            val OneLineReview = jsonObject.getString("OneLineReview")
                            val PloggingTime = jsonObject.getString("PloggingTime").toInt()
                            val trailID = jsonObject.getString("Trail_ID").toInt()
                            val PloggingSticker = jsonObject.getString("PloggingSticker").toInt()


                            val ploggingLogData = PloggingLogData(UserID, PloggingDate,
                                locationName, PloggingDistance, TrashStoragePhotos, OneLineReview,PloggingTime,trailID)
                            dataList.add(ploggingLogData)
                            // dataList에는 변환된 TrashLocationData 객체들이 들어있음
                            callback(dataList)
                        }

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    // 오류 처리
                    callback(null)
                }
            }

        }
    }
}
