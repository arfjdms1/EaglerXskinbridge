import requests
from PIL import Image
import io

img = Image.new('RGBA', (64, 64), color = (255, 0, 0, 255))
buf = io.BytesIO()
img.save(buf, format='PNG')
png_data = buf.getvalue()

files = {
    'file': ('skin.png', png_data, 'image/png'),
}
data = {
    'name': 'test-skin',
    'visibility': 'public'
}
headers = {
    'User-Agent': 'EaglercraftSkinBridge/v1.0'
}

response = requests.post('https://api.mineskin.org/generate/upload', files=files, data=data, headers=headers)
print(response.status_code)
print(response.text)
