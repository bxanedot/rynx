#!/usr/bin/env python3
"""Generate the independently authored silent MP3/cover test fixture; requires ffmpeg."""
from pathlib import Path
import subprocess
import struct, zlib
root=Path(__file__).resolve().parent.parent
p=root/'app/android/src/androidTest/assets';p.mkdir(parents=True,exist_ok=True)
# Original geometric test artwork, generated as a PNG; this is a photograph-free metadata fixture.
def chunk(kind,data): return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
w=h=32
pixels=b''.join(b'\0'+b''.join(bytes((238,65+x,112+y)) for x in range(w)) for y in range(h))
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b'')
cover=Path('/tmp/rynx-metadata-cover.png');cover.write_bytes(png)
subprocess.run(['ffmpeg','-y','-loglevel','error','-f','lavfi','-i','anullsrc=r=8000:cl=mono','-i',str(cover),'-t','1','-map','0:a','-map','1:v','-c:a','libmp3lame','-b:a','16k','-c:v','copy','-id3v2_version','3','-metadata','title=Evening walk','-metadata','artist=bxane','-metadata','album=Rynx test audio','-metadata:s:v','title=Album cover','-metadata:s:v','comment=Cover (front)',str(p/'tagged-silence.mp3')],check=True)
print('Generated original silent audio and geometric test artwork')
