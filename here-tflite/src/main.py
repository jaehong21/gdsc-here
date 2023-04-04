import math
import typing
from fastapi import FastAPI, UploadFile, File, HTTPException, Query
import uuid
import aiofiles
import os

from pydub import AudioSegment

# Imports
from tflite_support.task import audio
from tflite_support.task import core
from tflite_support.task import processor
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse
import orjson
import os

class ORJSONResponse(JSONResponse):
    media_type = "application/json"

    def render(self, content: typing.Any) -> bytes:
        return orjson.dumps(content)

app = FastAPI(default_response_class=ORJSONResponse)

@app.get("/alert")
def health_check():
    return "OK"


def mp4_2_wav(filename):
    """Convert mp4 to wav.
    Args:
        filename: mp4 file path.
    Returns:
        wav file path.
    """
    # convert mp4 to wav
    wav_file_path = filename.replace('.mp4', '.wav')
    new_sample_rate = 44100
    
    sound = AudioSegment.from_file(filename, format="mp4")
    sound = sound.set_channels(1)  # Convert to mono
    sound = sound.set_frame_rate(new_sample_rate) # Set Sample Rate. 
    sound = sound.set_sample_width(2) # Convert to 16-bit
    
    sound.export(wav_file_path, format="wav")
    num_channels = sound.channels
    return wav_file_path, num_channels

def stereo2mono(filename):
    # Open the stereo sound
    stereo_sound = AudioSegment.from_wav(filename)
    
    # Calling the split_to_mono() method on the stereo sound will return a tuple
    mono_audios = stereo_sound.split_to_mono()
    # Export the two mono channels as separate wav files
    mono_left = mono_audios[0].export(filename.replace('.wav', '_left.wav'), format="wav")
    return filename.replace('.wav', '_left.wav')

def nan_to_none(value):
    if isinstance(value, float) and math.isnan(value):
        return None
    else:
        return value

def check_extension(filename: str, valid_exts = ["mov", "mp4", "m4a", "3gp", "3g2", "mj2"]
):
    ext = os.path.splitext(filename)[-1][1:].lower()
    if ext not in valid_exts:
        return False
    return True


@app.post("/alert/predict")
async def get_file(file: UploadFile = File(...), keyword=Query('')):
    if len(file.filename) <= 0:
        raise HTTPException(status_code=400, detail="File not found")
    valid_exts = ["mov", "mp4", "m4a", "3gp", "3g2", "mj2"]
    if not check_extension(file.filename, valid_exts):
        raise HTTPException(status_code=400, detail=f"Invalid file extension. Must be one of {valid_exts}")
    filename = str(uuid.uuid4()) + file.filename
    filename = 'src/data/' + str(filename)
    async with aiofiles.open(filename, 'wb') as out_file:
        while content := await file.read(1024):  # async read chunk
            await out_file.write(content)  # async write chunk

    # Initialization
    model_path = "./converted_tflite/soundclassifier_with_metadata.tflite"
    base_options = core.BaseOptions(file_name=model_path)
    classification_options = processor.ClassificationOptions(max_results=2)
    options = audio.AudioClassifierOptions(base_options=base_options, classification_options=classification_options)
    classifier = audio.AudioClassifier.create_from_options(options)

    # Run inference
    mp4_audio_path = filename
    wav_audio_path, num_channels = mp4_2_wav(mp4_audio_path)
    if num_channels == 1:
        pass
    elif num_channels == 2:
        wav_audio_path = stereo2mono(wav_audio_path)
    else:
        raise HTTPException(status_code=400, detail="Audio channel not supported")
    
    audio_file = audio.TensorAudio.create_from_wav_file(wav_audio_path, classifier.required_input_buffer_size)
    audio_result = classifier.classify(audio_file)
    os.remove(mp4_audio_path)
    os.remove(wav_audio_path)

    print(audio_result)
    print("//////////////////////////")
    print(jsonable_encoder(audio_result, exclude_none=True))
    return jsonable_encoder(audio_result, exclude_none=True)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
