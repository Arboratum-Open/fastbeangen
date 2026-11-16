"""Run bounded existing examples against embedded HSQLDB and verify actual output."""
from pathlib import Path
import csv
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
if not __debug__:
    raise RuntimeError("Run this verification without Python optimization")
if len(sys.argv) != 2 or sys.argv[1] not in ("21", "25"):
    raise SystemExit("Usage: python3 verification/verify-examples.py 21|25")

command = [str(ROOT / "gradlew"), "--no-daemon", "--console=plain", "--max-workers=1", f"-PjavaVersion={sys.argv[1]}",
           ":fastbeangen-example:runGeneration", "--args=--persons 10 --relations 0 generate",
           ":fastbeangen-example:runQueryTest", "--args=--persons 10 --relations 0 query",
           ":fastbeangen-example:runCudAndQueryTest", "--args=--persons 10 --relations 0 cud",
           ":fastbeangen-example:runGenCVS", "--args=--persons 10000 --relations 20"]
result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=180)
output = result.stdout + result.stderr
log = ROOT / "build/verification/examples.log"
log.parent.mkdir(parents=True, exist_ok=True)
log.write_text(output)
if result.returncode:
    print("\n".join(output.splitlines()[-100:]))
    raise SystemExit(result.returncode)
failed_groups = re.findall(r"^KO: (.*)$", output, re.MULTILINE)
successful_groups = re.findall(r"^OK: LongSummaryStatistics\{count=([1-9][0-9]*),", output, re.MULTILINE)
assert len(failed_groups) >= 7 and all(group == "null" for group in failed_groups), log
assert len(successful_groups) == len(failed_groups), log
assert output.count("CUD checkpoint ") == 6, log
assert "exported the db 3 files" in output, log


def rows(name, count):
    with (ROOT / f"fastbeangen-example/build/{name}_0.csv").open(encoding="utf-8", newline="") as source:
        data = list(csv.reader(source))
    assert len(data) == count and all(len(row) == len(data[0]) for row in data), name
    return data


persons = rows("PERSON", 20000)
relations = rows("RELATION", 20)
links = rows("RELATION_PERSON", 47)
person_ids = {str(i) for i in range(10000)} | {str(10000000 + i) for i in range(10000)}
relation_ids = {str(i) for i in range(20)}
assert sum({row[column] for row in persons} == person_ids for column in range(len(persons[0]))) == 1
assert len(relations[0]) == 1 and {row[0] for row in relations} == relation_ids
assert len(links[0]) == 2
relation_columns = [column for column in range(2) if {row[column] for row in links} == relation_ids]
assert len(relation_columns) == 1
assert all(row[1 - relation_columns[0]] in person_ids for row in links)
print(f"Java {sys.argv[1]} examples passed: query, six CUD batches, 20000 CSV persons, 20 relations, 47 valid links")
print(f"Full output: {log}")
