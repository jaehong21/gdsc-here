import asyncio
import websockets

from src.utils import convert_mp4_to_mp3

async def send_file_to_websocket(file_path, websocket_uri):
    async with websockets.connect(websocket_uri) as websocket:
        while True: 
            with open(file_path, 'rb') as f:
                chunk = f.read(4096)
                if not chunk:
                    break
                await websocket.send(chunk)
                await asyncio.sleep(1)

            
        message = await websocket.recv()
        print(f'Received message from server: {message}')
        print("Done sending file")
            

# convert_mp4_to_mp3('hello.mp4')
asyncio.get_event_loop().run_until_complete(
     send_file_to_websocket('example.mp4', 'ws://localhost:8000/ws'))

