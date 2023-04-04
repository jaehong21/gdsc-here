package com.gdsc_gist.here

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.here.PermissionManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException

class RecodingService : Service() {
    private var fileName: String = ""

    private var recorder: MediaRecorder? = null
    private var counter: Int = 0;

    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mNotification: Notification.Builder

    private val nameNotificationId = 317

    private var userName: String? = null

    private var count = 0
    private var lastIndex = 0

    private var shouldStopLoop = false

    private val pauseLoopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            shouldStopLoop = true
            Log.d("here", "shouldStopLoop: $shouldStopLoop")
        }
    }

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter("ACTION_PAUSE_LOOP")
        LocalBroadcastManager.getInstance(this).registerReceiver(pauseLoopReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pauseLoopReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val sharedPreferences = getSharedPreferences("com.gdsc_gist.here", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", null)
        userName = name

        val channelId = "com.example.ServiceTest"
        val channelName = "MyServiceTestChannel"
        val notificationId = 316
        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val notification =
            NotificationCompat.Builder(this, channelId).setContentTitle("Here")
                .setContentText("Recognizing...").setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent).setTicker("ticker")
                .build()

        startForeground(notificationId, notification)


        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotification = Notification.Builder(this, channelId).setContentTitle("Here")
            .setContentText("내 이름이 불렸어요!")
            .setContentIntent(pendingIntent).setTicker("ticker2")

        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            while (true) {

                if(shouldStopLoop) {
                    delay(3000)
                    continue
                }

                startRecording(count)
                delay(3000)
                stopRecoding(count)

                if (count > 5) {
                    count = 0
                } else {
                    count++
                }
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRecording(count: Int) {

        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest-$count.mp4"
        recorder = MediaRecorder(this).apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioChannels(1)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recorder", "prepare $e failed")
            }

            start()
        }
    }

    private fun stopRecoding(count: Int) {
        recorder?.apply {
            stop()
            release()
        }

        val file = File("${externalCacheDir?.absolutePath}/audiorecordtest-$count.mp4")

        val requestFile = RequestBody.create("video/mp4".toMediaTypeOrNull(), file)

        val client =
            OkHttpClient.Builder().connectTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(100, java.util.concurrent.TimeUnit.SECONDS).build()

        val part = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestFile).build()

        val nameRequest =
            Request.Builder().url("http://here.jaehong21.com/name/predict?keyword=$userName").post(part)
                .build()
        client.newCall(nameRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Recorder", "Failed ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string()
                    Log.d("Recorder", "Success ${responseBodyString}")
                    if (responseBodyString != null) {
                        val json = JSONObject(responseBodyString)
                        val transcript = json.getString("transcript")
                        Log.d("Recorder", "Success $transcript")
                        if (transcript.contains(userName ?: "null")) {
                            mNotificationManager.notify(
                                nameNotificationId, mNotification
                                    .setSmallIcon(R.drawable.waving_hand)
                                    .build()
                            )
                        }
                    }
                } else {
                    Log.d("Recorder", "Failed ${response.code}")
                }
            }
        })

        val soundRequest =
            Request.Builder().url("http://here.jaehong21.com/alert/predict").post(part).build()

        client.newCall(soundRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Recorder", "Failed ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string()
                    Log.d("Recorder", "Success ${responseBodyString}")
                    if (responseBodyString != null) {
                        val json = JSONObject(responseBodyString)
                        val firstObject = json.getJSONArray("classifications").getJSONObject(0)
                        Log.d("Recorder", "Success $firstObject")
                        val category = firstObject.getJSONArray("categories").getJSONObject(0)
                        Log.d("Recorder", "Success $category")
                        val index = category.getString("index").toInt()
                        val score = category.getString("score").toFloat()
                        Log.d("Recorder", "Success $index")
                        if (score < 0.7) {
                            return
                        }
                        if (lastIndex == index) {
                            if (index in 1..7) {
                                mNotificationManager.notify(
                                    nameNotificationId, mNotification
                                        .setSmallIcon(R.drawable.waving_hand)
                                        .setLargeIcon(
                                            selectIconBitmap(
                                                index,
                                                this@RecodingService.resources
                                            )
                                        )
                                        .setContentText(selectNotyText(index))
                                        .build()
                                )
                            }
                        } else {
                            lastIndex = index
                        }
                    }
                } else {
                    Log.d("Recorder", "Failed ${response.code}")
                }
            }
        })

        counter++
        recorder = null
    }

    private fun selectIconBitmap(index: Int, resources: Resources): Bitmap {
        return when (index) {
            1 -> BitmapFactory.decodeResource(resources, R.drawable.ambulance)
            2 -> BitmapFactory.decodeResource(resources, R.drawable.police_car)
            3 -> BitmapFactory.decodeResource(resources, R.drawable.virus)
            4 -> BitmapFactory.decodeResource(resources, R.drawable.bomb)
            5 -> BitmapFactory.decodeResource(resources, R.drawable.emergency_exit)
            6 -> BitmapFactory.decodeResource(resources, R.drawable.civil_defense)
            7 -> BitmapFactory.decodeResource(resources, R.drawable.fire_truck)
            else -> BitmapFactory.decodeResource(resources, R.drawable.waving_hand)
        }
    }

    private fun selectNotyText(index: Int): String {
        return when (index) {
            1 -> "구급차 소리가 들려요!"
            2 -> "경찰차 소리가 들려요!"
            3 -> "감염 경보가 들려요!"
            4 -> "공습 경보가 들려요!"
            5 -> "대피 경보가 들려요!"
            6 -> "민방위 경보가 들려요!"
            7 -> "소방차 소리가 들려요!"
            else -> ""
        }
    }
}