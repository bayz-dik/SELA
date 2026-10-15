#!/usr/bin/env python3
"""Verify debug APK metadata, ZIP integrity, alignment and signature."""
from pathlib import Path
import hashlib, os, re, subprocess, zipfile
root = Path(__file__).resolve().parents[1]
apk = root/'app/build/outputs/apk/debug/app-debug.apk'
sdk = Path('/home/hatch/.cache/sela-android-sdk/build-tools/34.0.0')
env = dict(os.environ, JAVA_HOME='/home/hatch/.cache/sela-toolchains/jdk')
def run(*args):
    result = subprocess.run(list(map(str, args)), check=True, capture_output=True, text=True, env=env)
    return result.stdout
badging = run(sdk/'aapt', 'dump', 'badging', apk)
assert "package: name='id.sela'" in badging
assert "versionCode='1'" in badging and "versionName='0.4.0'" in badging
assert "application-label:'SELA'" in badging
assert "launchable-activity: name='id.sela.MainActivity'" in badging
assert "sdkVersion:'26'" in badging
assert 'android.permission.ACCESS_BACKGROUND_LOCATION' not in badging
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    names = z.namelist()
    assert 'AndroidManifest.xml' in names and 'classes.dex' in names
    native = [name for name in names if name.startswith('lib/') and name.endswith('.so')]
    assert any('arm64-v8a' in name for name in native)
signature = run(sdk/'apksigner', 'verify', '--verbose', apk)
alignment = run(sdk/'zipalign', '-c', '4', apk)
checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
print(badging)
print(signature)
print('ZIP integrity and zipalign: PASS')
print('Native libraries:', ', '.join(native))
print('APK:', apk)
print('Bytes:', apk.stat().st_size)
print('SHA256:', checksum)
print('Structural/install-package checks passed; no device installation claimed.')
