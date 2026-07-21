"""Run the golden-set regression eval against the current writing system prompt.

Usage (from `bff/youtube_scraper/`):
    python -m eval.run_eval

Loads `eval/golden_set.json`, calls the real `evaluate_essay_with_ai` for each
entry, and prints a pass-rate / average-deviation report.  The active prompt
version is whatever `app.services.llm_service.ACTIVE_PROMPT_VERSION` resolves to.
"""
import asyncio
import json
import logging
import os
import statistics
import sys
from pathlib import Path
from typing import Any

# Load .env if present so this script is runnable outside docker-compose.
try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).resolve().parent.parent / ".env")
except ImportError:  # pragma: no cover
    pass

# Allow running as `python -m eval.run_eval` from bff/youtube_scraper.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.services.llm_service import ACTIVE_PROMPT_VERSION, evaluate_essay_with_ai  # noqa: E402

logging.basicConfig(level=logging.WARNING, format="%(levelname)s: %(message)s")
logger = logging.getLogger("eval")

GOLDEN_SET_PATH = Path(__file__).resolve().parent / "golden_set.json"
TOLERANCE = 0.5  # a predicted band within ±0.5 of expected counts as a pass


async def run_single_entry(entry: dict[str, Any]) -> dict[str, Any]:
    """Evaluate one golden-set entry and return a result row."""
    entry_id = entry["id"]
    expected = float(entry["expected_band"])
    try:
        evaluation = await evaluate_essay_with_ai(
            task_prompt=entry["task_prompt"],
            essay_text=entry["essay_text"],
        )
        actual = evaluation.overall_band
        error: str | None = None
    except Exception as e:  # noqa: BLE001
        actual = float("nan")
        error = str(e)
    return {
        "id": entry_id,
        "expected": expected,
        "actual": actual,
        "delta": abs(actual - expected) if actual == actual else float("nan"),
        "passed": (abs(actual - expected) <= TOLERANCE) if actual == actual else False,
        "error": error,
    }


def print_report(results: list[dict[str, Any]]) -> None:
    """Print a pass-rate + average-deviation report."""
    ok = [r for r in results if r["error"] is None]
    failed = [r for r in results if r["error"] is not None]
    passed = [r for r in ok if r["passed"]]
    deltas = [r["delta"] for r in ok]

    print()
    print("=" * 70)
    print(f"Golden-set regression eval  |  prompt version: {ACTIVE_PROMPT_VERSION}")
    print("=" * 70)
    print(f"Entries: {len(results)}  |  Passed (within ±{TOLERANCE}): {len(passed)}"
          f"  |  Errors: {len(failed)}")
    if deltas:
        print(f"Pass rate:  {len(passed) / len(results) * 100:.1f}%")
        print(f"Avg delta:  {statistics.mean(deltas):.2f}")
        print(f"Max delta:  {max(deltas):.2f}")
        print(f"Med delta:  {statistics.median(deltas):.2f}")
    print("-" * 70)
    print(f"{'ID':<32} {'Expected':>9} {'Actual':>9} {'Δ':>6} {'Pass':>6}")
    for r in results:
        actual_s = f"{r['actual']:.1f}" if r["actual"] == r["actual"] else "ERR"
        print(f"{r['id']:<32} {r['expected']:>9.1f} {actual_s:>9} "
              f"{r['delta']:>6.2f} {('✓' if r['passed'] else '✗'):>6}")
    if failed:
        print("-" * 70)
        print("Errors:")
        for r in failed:
            print(f"  {r['id']}: {r['error']}")
    print("=" * 70)


async def main() -> int:
    if not GOLDEN_SET_PATH.exists():
        print(f"Missing golden set: {GOLDEN_SET_PATH}", file=sys.stderr)
        return 1
    if not os.getenv("LLM_API_KEY"):
        print("LLM_API_KEY is not set — cannot call the grading function.", file=sys.stderr)
        return 1

    entries = json.loads(GOLDEN_SET_PATH.read_text())
    print(f"Running {len(entries)} entries against prompt {ACTIVE_PROMPT_VERSION}...")

    # Sequential to keep within rate limits and produce deterministic logs.
    results: list[dict[str, Any]] = []
    for entry in entries:
        result = await run_single_entry(entry)
        results.append(result)
        actual_s = f"{result['actual']:.1f}" if result["actual"] == result["actual"] else "ERR"
        print(f"  [{len(results):>2}/{len(entries)}] {result['id']:<32}"
              f" expected={result['expected']:.1f}  actual={actual_s}")

    print_report(results)
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
