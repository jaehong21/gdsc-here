# Here

This is a repository for the Google Developer Student Club at the Gwangju Institute of Science and Technology (GIST).

```shell
.
├── README.md
├── here-android
│   ├── app
│   ├── build.gradle
│   ├── gradle
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle
├── here-be
│   ├── Dockerfile
│   ├── poetry.lock
│   ├── pyproject.toml
│   └── src
└── here-tflite
    ├── Dockerfile
    ├── converted_tflite
    ├── poetry.lock
    ├── pyproject.toml
    └── src
```

## here-android

The android application project directory is located at here-android. The app is built using Android Studio.

Main files are located at **`gdsc-here/here-android/app/src/main/java/com/example/here/`**

1. Clone this repository
2. Open the here-android directory in Android Studio
3. Build the project
4. Run the project on a physical device that has a microphone.
   (Connect a wear os device to the physical device to get alerts on the wear os device)
5. Follow the instructions on the screen to get started.
6. Now, you will get notifications when someone is calling your name or when some alarm is detected.

<br />

## here-be, here-tflite

This folder includes poetry projects with backend server using FastAPI. Its project is Dockerized and being deployed in GCP.

Address: http://here.jaehong21.com <br />

**Health Check API**

```shell
curl --location 'http://here.jaehong21.com/name'
curl --location 'http://here.jaehong21.com/alert'
```

<br />

**Speech To Text API** <br />

```shell
curl --location 'http://here.jaehong21.com/name/predict' \
--form 'file=@"audiorecordtest-0.mp4"'
```

```json
{
  "transcript": "사람이 말하면 그거 따라서",
  "confidence": 0.6189181208610535
}
```

<br />

**Alert Classification API** <br />

```shell
curl --location 'http://here.jaehong21.com/alert/predict' \
--form 'file=@"audiorecordtest-0.mp4"'
```

```json
{
  "classifications": [
    {
      "categories": [
        {
          "index": 8,
          "score": 0.597916305065155,
          "display_name": "",
          "category_name": "8 카페"
        },
        {
          "index": 0,
          "score": 0.08570388704538345,
          "display_name": "",
          "category_name": "0 Background Noise"
        }
      ],
      "head_index": 0,
      "head_name": "probability"
    }
  ]
}
```
