package com.gdsc_gist.here

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.here.PermissionManager
import com.gdsc_gist.here.ui.theme.HereTheme
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KSuspendFunction1


class InitialActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    private val nameSaver by lazy {
        NameSaver(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("com.gdsc_gist.here", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", null)

        permissionManager = PermissionManager(this)
        permissionManager.checkPermissions()

        setContent {
            HereTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    HomeScreen(saveName = nameSaver::saveName)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}

class NameSaver(private val context: Context) {
    suspend fun saveName(name: String) {
        try {

            val sharedPreferences =
                context.getSharedPreferences("com.gdsc_gist.here", Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            editor.putString("name", name)
            editor.apply()

        } catch (cancellationException: CancellationException) {
            Log.e("NameSaver", "Error saving name", cancellationException)
        } catch (exception: Exception) {
            Log.e("NameSaver", "Error saving name", exception)
        }
    }
}

@Composable
fun HomeScreen(saveName: KSuspendFunction1<String, Unit>) {
    var stage by remember { mutableStateOf(0 as Int) }
    var name by remember { mutableStateOf("") }

    Box {
        StageContent(
            stage = stage,
            name = name,
            onNameChange = { name = it },
            onStageAdvance = { stage++ },
            onStageRegress = { stage-- },
            saveName = saveName
        )

        if (stage == 4) {
            RecordingScreen(advanceStage = { stage++ }, name = name)
        }
    }
}

@Composable
fun StageContent(
    stage: Int,
    name: String,
    onNameChange: (String) -> Unit,
    onStageAdvance: () -> Unit,
    onStageRegress: () -> Unit,
    saveName: KSuspendFunction1<String, Unit>
) {
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF242424))
            .clickable {
                when (stage) {
                    0, 1 -> onStageAdvance()
                    2 -> {
                        if (name != "") {
                            onStageAdvance()
                        }
                    }
                    3 -> onStageAdvance()
                    5 -> {
                        context.startActivity(Intent(context, AlertConfigureActivity::class.java))
                    }
                    else -> {
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ImageSection(stage = stage)
        if (stage == 2) {
            NameInput(name = name, onNameChange = onNameChange)
        } else {
            TalkingText(stage, name)
        }

    }
    if (stage == 3) {
        ConfirmationButtons(
            onConfirm = { runBlocking { launch { saveName(name) }.join() }; onStageAdvance() },
            onCancel = onStageRegress
        )
    }
}

@Composable
fun NameInput(name: String, onNameChange: (String) -> Unit) {
    BasicTextField(
        value = name, onValueChange = onNameChange,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        ),
        decorationBox = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                it()
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(270.dp)
                        .background(Color.White)
                ) {}
            }
        },
    )
}

@Composable
fun ConfirmationButtons(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(bottom = 37.dp)
            .fillMaxSize()
            .offset(x = 0.dp, y = 0.dp)

    ) {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF30D158)),
            modifier = Modifier
                .height(64.dp)
                .width(158.dp)
                .clip(shape = RoundedCornerShape(15.dp))
        ) {
            Text(text = "Yes", fontSize = 30.sp, fontWeight = FontWeight.W600)
        }
        Spacer(modifier = Modifier.width(13.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF453A)),
            modifier = Modifier
                .height(64.dp)
                .width(158.dp)
                .clip(shape = RoundedCornerShape(15.dp))

        ) {
            Text(text = "No", fontSize = 30.sp, fontWeight = FontWeight.W600)
        }
    }
}

@Composable
fun RecordingScreen(advanceStage: () -> Unit, name: String) {
    var checked by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            while (true) {
                val response = sendRecording(context, name)
                Log.d("RecordingScreen", "Response: $response")
                checked = response != null
                if (checked) {
                    delay(1000)
                    advanceStage()
                }
            }
        }
    }

    if (checked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 0.dp, y = 0.dp)
                .background(Color(0x7F242424)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.check_green),
                contentDescription = "Green Check"
            )
        }
    }
}


suspend fun sendRecording(context: Context, name: String): String? {
    val fileName = "${context.externalCacheDir?.absolutePath}/audiorecordtest.mp4"
    val recorder = MediaRecorder(context).apply {
        setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(fileName)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        try {
            prepare()
        } catch (e: IOException) {
            Log.e("Recorder", "prepare $e failed")
            return null
        }

        start()
    }

    delay(3000)

    recorder.stop()
    recorder.release()

    val file = File(fileName)

    return withContext(Dispatchers.IO) {
        val requestFile = RequestBody.create("video/mp4".toMediaTypeOrNull(), file)

        val client = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .build()

        val part = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestFile)
            .build()

        val request = Request.Builder()
            .url("http://here.jaehong21.com/name/predict?keyword=$name")
            .post(part)
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBodyString = response.body?.string()
            Log.d("Recorder", "Success ${responseBodyString}")
            responseBodyString?.let {
                val json = JSONObject(it)
                val transcript = json.getString("transcript")
                Log.d("Recorder", "Success $transcript")
                if (transcript.contains(name)) {
                    transcript
                } else {
                    transcript
                }
            }
        } else {
            Log.d("Recorder", "Failed ${response.code}")
            "skip"
        }
    }
}

@Composable
fun ImageSection(stage: Int) {
    Row(
        modifier = Modifier
            .padding(bottom = 20.dp)
    ) {
        when (stage) {
            0, 1, 2 -> Image(
                painter = painterResource(R.drawable.waving_hand),
                contentDescription = "Waving hand",
                modifier = Modifier.size(120.dp)
            )
            3 -> Image(
                painter = painterResource(R.drawable.grinning_face),
                contentDescription = "Grinning face",
                modifier = Modifier.size(120.dp)
            )
            4 -> Image(
                painter = painterResource(R.drawable.sound_graph),
                contentDescription = "Sound Graph",
                modifier = Modifier.size(294.dp)
            )
            5 -> Image(
                painter = painterResource(R.drawable.sunglasses_face),
                contentDescription = "Sunglasses face",
                modifier = Modifier.size(120.dp)
            )
            else -> {}
        }
    }
}

@Composable
fun TalkingText(stage: Int, name: String) {
    val text = getTextForStage(stage, name)

    Text(text = text, fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Black)
}

fun getTextForStage(stage: Int, name: String): String {
    return when (stage) {
        0 -> "Hi there!"
        1 -> "Can you tell me your name?"
        2 -> ""
        3 -> "Is your name $name?"
        4 -> "Call '$name'"
        5 -> "Now you are all set"
        else -> ""
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HereTheme {
    }
}
