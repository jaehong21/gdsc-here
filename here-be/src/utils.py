from moviepy.editor import *
# import subprocess

def convert_mp4_to_mp3(input_path):
    # command = f"ffmpeg -i {input_path} -vn -acodec libmp3lame {output_path}"
    # subprocess.call(command, shell=True)
    clip = AudioFileClip(input_path)
    # Extract the audio from the video and save it as MP3
    output_path = input_path[:-4] + ".mp3"
    clip.write_audiofile(output_path)
    
    # Close the video and audio files
    clip.close()

    return output_path
