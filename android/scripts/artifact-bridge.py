#!/usr/bin/env python3
"""Loopback-only curl bridge; no credentials stored. Official upstreams only."""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
import hashlib, subprocess, threading
CACHE=Path.home()/'.cache/sela-android-artifacts'; CACHE.mkdir(parents=True,exist_ok=True)
ORIGINS={'google':'https://dl.google.com/dl/android/maven2/','central':'https://repo.maven.apache.org/maven2/','plugins':'https://plugins.gradle.org/m2/','sdk':'https://dl.google.com/android/repository/'}
locks={}; guard=threading.Lock()
class Handler(BaseHTTPRequestHandler):
 def do_HEAD(self): self.serve(False)
 def do_GET(self): self.serve(True)
 def serve(self,body):
  bits=self.path.strip('/').split('/',1)
  if len(bits)!=2 or bits[0] not in ORIGINS or '..' in self.path: self.send_error(400); return
  key=hashlib.sha256(self.path.encode()).hexdigest(); target=CACHE/key
  with guard: lock=locks.setdefault(key,threading.Lock())
  with lock:
   if not target.exists():
    tmp=target.with_suffix('.part')
    r=subprocess.run(['curl','-fsSL','--retry','2','--max-time','120',ORIGINS[bits[0]]+bits[1],'-o',str(tmp)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    if r.returncode: tmp.unlink(missing_ok=True); self.send_error(404); return
    tmp.replace(target)
  self.send_response(200);self.send_header('Content-Length',str(target.stat().st_size));self.end_headers()
  if body:
   try:
    with target.open('rb') as f:
     while chunk:=f.read(65536):self.wfile.write(chunk)
   except (BrokenPipeError,ConnectionResetError):pass
 def log_message(self,*args):pass
print('Android artifact bridge 127.0.0.1:8766',flush=True)
ThreadingHTTPServer(('127.0.0.1',8766),Handler).serve_forever()
