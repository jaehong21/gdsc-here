import io
from typing import List

from google.cloud import speech

def speech_to_text(path: str, keywords: List[str]):
    # elapsed time next line
    client = speech.SpeechClient.from_service_account_json('gdsc-here.json')
    # Loads the audio into memory
    with io.open((path), 'rb') as audio_file:
        content = audio_file.read()
        audio = speech.RecognitionAudio(content=content)

    # Set speech context for keyword spotting
    speech_context = speech.SpeechContext(
        phrases= keywords,
        boost = 20.0
    )

    # Configure the speech recognition
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED,
        sample_rate_hertz=16000,
        language_code="ko-KR",
        speech_contexts=[speech_context]
    )

    # Detects speech in the audio file
    response = client.recognize(config=config, audio=audio)
    print("response", response)

    if response.results:
        return {
            "transcript": response.results[0].alternatives[0].transcript,
            "confidence": response.results[0].alternatives[0].confidence
        }
    else:
        return {
            "transcript": None,
            "confidence": 0
        }
    
    # for result in response.results:
    #     for keyword in keywords:    
    #         if keyword in result.alternatives[0].transcript:
    #             print(f"Detected {keywords}")
    
    
