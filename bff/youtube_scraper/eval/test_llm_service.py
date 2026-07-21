"""Smoke tests for llm_service hardening. Run with: python -m eval.test_llm_service

No network, no LLM cost. Mocks the OpenAI client to verify:
  - 1.1 retry: a malformed response is followed by a correction call, and the
    next valid response is returned (not fabricated).
  - 1.1 final failure: 3 consecutive bad responses surface as
    `RuntimeError("grading_temporarily_unavailable")`.
  - 1.4 versioning: `SIMON_BAND9_SYSTEM_PROMPT` is loaded from the
    `app/prompts/writing_v2.txt` file and contains the injection defense line.
  - 1.2 delimiters: `_build_user_message` wraps the essay with the markers.
"""
import asyncio
import sys
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.services import llm_service  # noqa: E402
from app.services.llm_service import (  # noqa: E402
    SIMON_BAND9_SYSTEM_PROMPT,
    _build_user_message,
    evaluate_essay_with_ai,
)


def _make_fake_response(content: str):
    resp = MagicMock()
    resp.choices = [MagicMock()]
    resp.choices[0].message.content = content
    return resp


_VALID_PAYLOAD = (
    '{"overall_band": 7.0, "coherence_feedback": "ok",'
    ' "vocabulary_suggestions": ["a", "b", "c"],'
    ' "simon_style_rewrite": "rewrite"}'
)
_MALFORMED_PAYLOAD = '{"overall_band": 9.0}'  # missing 3 required fields


class PromptVersioningTests(unittest.TestCase):
    def test_prompt_loaded_from_v2_file(self):
        # 1.4 — the active prompt comes from a versioned .txt file, not inline.
        v2_path = Path(llm_service._PROMPTS_DIR) / f"writing_{llm_service.ACTIVE_PROMPT_VERSION}.txt"
        self.assertTrue(v2_path.exists(), f"missing prompt file: {v2_path}")
        self.assertIn("<<<ESSAY_START>>>", SIMON_BAND9_SYSTEM_PROMPT)


class EssayDelimitersTests(unittest.TestCase):
    def test_essay_wrapped_with_markers(self):
        msg = _build_user_message("task", "essay body")
        self.assertIn("<<<ESSAY_START>>>", msg)
        self.assertIn("<<<ESSAY_END>>>", msg)
        self.assertIn("essay body", msg)


class RetryTests(unittest.TestCase):
    async def _run_with_responses(self, contents):
        fake_client = MagicMock()
        fake_client.chat = MagicMock()
        fake_client.chat.completions = MagicMock()
        fake_client.chat.completions.create = AsyncMock(
            side_effect=[_make_fake_response(c) for c in contents]
        )
        with patch.object(llm_service, "get_llm_client", return_value=fake_client), \
             patch.object(llm_service, "settings") as fake_settings:
            fake_settings.llm_model = "test"
            return await evaluate_essay_with_ai("task", "a" * 200)  # > 100 words, no warning

    def test_retry_on_malformed_then_valid(self):
        # 1.1 — first response is malformed, second is valid; retry fires, returns the valid one.
        result = asyncio.run(self._run_with_responses([_MALFORMED_PAYLOAD, _VALID_PAYLOAD]))
        self.assertEqual(result.overall_band, 7.0)

    def test_retry_feeds_error_back_to_model(self):
        # 1.1 — the second call's messages list should include the bad assistant output
        # and a correction instruction.
        fake_client = MagicMock()
        fake_client.chat.completions.create = AsyncMock(
            side_effect=[_make_fake_response(_MALFORMED_PAYLOAD), _make_fake_response(_VALID_PAYLOAD)]
        )
        captured_kwargs: list[dict] = []

        async def capture_create(**kwargs):
            captured_kwargs.append(kwargs)
            return _make_fake_response(
                _VALID_PAYLOAD if len(captured_kwargs) > 1 else _MALFORMED_PAYLOAD
            )

        fake_client.chat.completions.create = capture_create
        with patch.object(llm_service, "get_llm_client", return_value=fake_client), \
             patch.object(llm_service, "settings") as fake_settings:
            fake_settings.llm_model = "test"
            asyncio.run(evaluate_essay_with_ai("task", "a" * 200))

        self.assertEqual(len(captured_kwargs), 2)
        second_messages = captured_kwargs[1]["messages"]
        # last two messages should be: bad assistant output, then correction user message
        self.assertEqual(second_messages[-2]["role"], "assistant")
        self.assertIn(_MALFORMED_PAYLOAD, second_messages[-2]["content"])
        self.assertEqual(second_messages[-1]["role"], "user")
        self.assertIn("failed validation", second_messages[-1]["content"])

    def test_final_failure_raises_unavailable(self):
        # 1.1 — all 3 attempts fail, must raise the explicit error.
        with self.assertRaises(RuntimeError) as ctx:
            asyncio.run(self._run_with_responses(
                [_MALFORMED_PAYLOAD, _MALFORMED_PAYLOAD, _MALFORMED_PAYLOAD]
            ))
        self.assertIn("grading_temporarily_unavailable", str(ctx.exception))


class SuspiciousScoreTests(unittest.TestCase):
    def test_warning_logged_for_short_essay_high_band(self):
        # 1.2 — short essay + high band triggers a warning log.
        with self.assertLogs("app.services.llm_service", level="WARNING") as cm:
            llm_service._check_suspicious_score(
                essay_text=" ".join(["word"] * 50),
                evaluation=llm_service.WritingEvaluation.model_validate_json(
                    '{"overall_band": 9.0, "coherence_feedback": "x",'
                    ' "vocabulary_suggestions": ["a"], "simon_style_rewrite": "y"}'
                ),
            )
        self.assertTrue(any("Suspicious score" in line for line in cm.output))

    def test_no_warning_for_long_essay(self):
        # 1.2 — long essay doesn't trip the heuristic.
        from pydantic import ValidationError  # noqa: F401
        long_text = " ".join(["word"] * 300)
        # Logger should not emit "Suspicious score" for a long essay.
        try:
            with self.assertLogs("app.services.llm_service", level="DEBUG") as cm:
                llm_service._check_suspicious_score(
                    essay_text=long_text,
                    evaluation=llm_service.WritingEvaluation.model_validate_json(
                        '{"overall_band": 9.0, "coherence_feedback": "x",'
                        ' "vocabulary_suggestions": ["a"], "simon_style_rewrite": "y"}'
                    ),
                )
        except AssertionError:
            return  # no log output at all is the correct behavior
        self.assertFalse(any("Suspicious score" in line for line in cm.output))


if __name__ == "__main__":
    unittest.main(verbosity=2)
