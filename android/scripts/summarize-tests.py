#!/usr/bin/env python3
"""Read Gradle JUnit XML, reject incomplete/failing module results."""
from pathlib import Path
import json
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
modules = ['app', 'core/model', 'core/database', 'core/network', 'core/location', 'core/designsystem', 'feature/map', 'feature/search', 'feature/settings']
summary = {}
for module in modules:
    paths = sorted((root/module/'build/test-results').glob('*/TEST-*.xml'))
    seen = set()
    counts = {'executions': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    variants = {}
    for path in paths:
        suite = ET.parse(path).getroot()
        tests = suite.findall('testcase')
        assert len(tests) == int(suite.attrib['tests']), str(path)
        counts['executions'] += len(tests)
        for key in ['failures', 'errors', 'skipped']:
            counts[key] += int(suite.attrib.get(key, 0))
        variants[path.parent.name] = variants.get(path.parent.name, 0) + len(tests)
        seen.update((test.attrib['classname'], test.attrib['name']) for test in tests)
    counts['unique_tests'] = len(seen)
    counts['variants'] = variants
    summary[module] = counts
print(json.dumps(summary, indent=2))
assert all(row['unique_tests'] > 0 for row in summary.values()), 'A registered module has no tests'
assert all(not row['failures'] and not row['errors'] and not row['skipped'] for row in summary.values()), 'Unsuccessful tests present'
for module, row in summary.items():
    expected = {'test'} if module == 'core/model' else {'testDebugUnitTest', 'testReleaseUnitTest'}
    assert set(row['variants']) == expected, f'{module}: missing test variant'
print('TOTAL unique_tests:', sum(row['unique_tests'] for row in summary.values()))
print('TOTAL executions:', sum(row['executions'] for row in summary.values()))
