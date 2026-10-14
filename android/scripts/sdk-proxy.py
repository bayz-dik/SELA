#!/usr/bin/env python3
"""SDK manager's --no_https forward proxy: curl still verifies HTTPS upstream."""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlsplit
import subprocess
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  u=urlsplit(self.path)
  if u.hostname != 'dl.google.com' or not u.path.startswith('/android/repository/'):
   self.send_error(403); return
  result=subprocess.run(['curl','-fsSL','--max-time','180','--retry','2','https://dl.google.com'+u.path],capture_output=True)
  if result.returncode:self.send_error(404);return
  self.send_response(200);self.send_header('Content-Length',str(len(result.stdout)));self.end_headers()
  try:self.wfile.write(result.stdout)
  except (BrokenPipeError,ConnectionResetError):pass
 def log_message(self,*args):pass
print('SDK proxy 127.0.0.1:8767',flush=True)
ThreadingHTTPServer(('127.0.0.1',8767),Handler).serve_forever()
