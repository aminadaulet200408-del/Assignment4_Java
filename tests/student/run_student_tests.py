import json, pathlib, subprocess, sys
P=pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0,str(P/'tools'))
from public_validate import check
source=P/'Java'/'source'; build=P/'Java'/'build'; build.mkdir(exist_ok=True)
subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',str(build),*map(str,sorted(source.glob('*.java')))],check=True)
cases=json.loads((P/'tests'/'student'/'expected.json').read_text(encoding='utf-8'))
payload=''.join(json.dumps(c['input'],separators=(',',':'))+'\n' for c in cases)
result=subprocess.run(['java','-Xmx384m','-cp',str(build),'Main'],input=payload,text=True,capture_output=True,check=True)
lines=result.stdout.splitlines()
assert len(lines)==len(cases), f'expected {len(cases)} lines, got {len(lines)}'
for i,(case,line) in enumerate(zip(cases,lines),1):
    answer=json.loads(line)
    assert check(case['input'],answer,case['output']), f'failed student fixture {i}: {case["input"]["task"]}'
print(f'PASS: {len(cases)} student fixtures')
