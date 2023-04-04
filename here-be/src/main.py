from fastapi import FastAPI, UploadFile, File, HTTPException, Query, WebSocket, WebSocketDisconnect
import uuid
import aiofiles
# import asyncio
import os

from src.utils import convert_mp4_to_mp3
from src.google_speech import speech_to_text

app = FastAPI()


@app.get("/name")
def health_check():
    return "OK"

@app.post("/name/predict")
async def get_file(file: UploadFile = File(...), keyword=Query('')):
    if len(file.filename) <= 0:
        raise HTTPException(status_code=400, detail="File not found")
    # if file.filename.endswith(".mp4") is False:
    #     raise HTTPException(status_code=400, detail="File must be mp4")

    mp4_file = str(uuid.uuid4()) + file.filename
    mp4_file = 'src/data/' + str(mp4_file)
    async with aiofiles.open(mp4_file, 'wb') as out_file:
        while content := await file.read(1024):  # async read chunk
            await out_file.write(content)  # async write chunk

    # convert mp4 to mp3
    mp3_file = convert_mp4_to_mp3(mp4_file)
    os.remove(mp4_file)

    print("keyword: ", keyword)
    results = speech_to_text(mp3_file, [keyword])
    os.remove(mp3_file)
    return results


# @app.websocket("/ws")
# async def websocket_endpoint(websocket: WebSocket):
#     await websocket.accept()
#     while True:
#         try:
#             filename = 'hello' + '.mp4'
#             while True:
#                 # await websocket.send_text(f"Received: {len(data)} bytes")
                
#                 async with aiofiles.open(filename, 'wb') as out_file:
#                     data = await websocket.receive_bytes()
#                     await out_file.write(data)
#                     if len(data) < 4096:
#                         break


#             # filename = 'src/data/' + filename
#             # mp3_file = convert_mp4_to_mp3(filename)
#             # os.remove(filename)

#             # results = speech_to_text(mp3_file, [])
#             # os.remove(mp3_file)
#             # print(str(results))
#             # await websocket.send_text(str(results))
#             await websocket.send_json({
#                 "message": "Received file: " + filename
#             })
    
#         except WebSocketDisconnect:
#             break

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
