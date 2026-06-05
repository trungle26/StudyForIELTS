const CLASSIFIER_VERSION = "local-readability-v1";
const LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"];

const CEFR_VOCABULARY = {
  A1: [
    "a", "about", "and", "animal", "answer", "apple", "ask", "bad", "be", "beautiful",
    "because", "big", "book", "boy", "buy", "can", "car", "child", "city", "class",
    "come", "day", "do", "eat", "family", "friend", "get", "girl", "go", "good",
    "happy", "have", "home", "house", "i", "know", "like", "listen", "live", "make",
    "man", "many", "money", "mother", "name", "new", "not", "now", "people", "play",
    "read", "school", "see", "small", "speak", "student", "teacher", "time", "want", "water",
    "way", "week", "woman", "work", "write", "you"
  ],
  A2: [
    "accident", "airport", "alone", "already", "arrive", "article", "believe", "business", "careful", "cheap",
    "choose", "college", "comfortable", "conversation", "customer", "different", "difficult", "direction", "doctor", "early",
    "enough", "environment", "explain", "favorite", "foreign", "future", "holiday", "important", "information", "internet",
    "journey", "language", "library", "machine", "market", "meeting", "message", "minute", "modern", "museum",
    "necessary", "office", "opinion", "possible", "problem", "quiet", "reason", "remember", "restaurant", "return",
    "simple", "special", "station", "suggest", "ticket", "travel", "usually", "weather", "without", "yesterday"
  ],
  B1: [
    "achieve", "advantage", "although", "application", "arrangement", "available", "behavior", "challenge", "compare", "concern",
    "condition", "confirm", "consider", "continue", "culture", "decision", "degree", "develop", "difference", "education",
    "effective", "encourage", "experience", "factor", "government", "however", "improve", "include", "individual", "industry",
    "influence", "instead", "international", "introduce", "knowledge", "manager", "method", "opportunity", "organization", "percent",
    "personal", "population", "prepare", "probably", "provide", "quality", "relationship", "require", "research", "resource",
    "responsible", "result", "similar", "situation", "society", "support", "therefore", "training", "understand", "various"
  ],
  B2: [
    "accurate", "alternative", "approach", "assessment", "assumption", "authority", "benefit", "capacity", "circumstance", "complex",
    "conclusion", "consequence", "consistent", "context", "contrast", "criteria", "debate", "demonstrate", "despite", "economic",
    "emphasis", "evidence", "financial", "flexible", "function", "identify", "impact", "interpret", "issue", "maintain",
    "majority", "network", "perspective", "potential", "previous", "principle", "priority", "process", "professional", "region",
    "regulation", "relevant", "reliable", "significant", "specific", "strategy", "structure", "sufficient", "technical", "widespread"
  ],
  C1: [
    "abstract", "acknowledge", "advocate", "ambiguous", "analytical", "anticipate", "attribute", "comprehensive", "controversial", "criterion",
    "deduce", "differentiate", "dimension", "discourse", "emerge", "empirical", "equivalent", "establish", "evaluate", "explicit",
    "framework", "implicit", "inherent", "innovation", "insight", "integrate", "justify", "legislation", "mechanism", "nevertheless",
    "notion", "objective", "phenomenon", "precise", "predominantly", "prospect", "qualitative", "reinforce", "scope", "subsequent",
    "sustain", "theoretical", "transition", "underlying", "valid", "whereas"
  ],
  C2: [
    "aberration", "acquiesce", "alleviate", "anomaly", "arbitrary", "catalyst", "coherent", "conjecture", "convoluted", "dichotomy",
    "elucidate", "exacerbate", "formidable", "inadvertent", "juxtapose", "meticulous", "nuanced", "paradigm", "pervasive", "plausible",
    "pragmatic", "profound", "reconcile", "scrutinize", "substantiate", "tenuous", "ubiquitous"
  ]
};

const WORD_LEVEL = new Map(
  Object.entries(CEFR_VOCABULARY).flatMap(([level, words]) => words.map((word) => [word, level]))
);

const ADVANCED_SUFFIXES = [
  "tion", "sion", "ment", "ance", "ence", "ity", "ism", "ology", "ative", "istic", "ization"
];

export function classifyCefr(transcriptText) {
  const words = tokenizeWords(transcriptText);
  const sentences = splitSentences(transcriptText);

  if (words.length < 30) {
    return {
      level: "A1",
      confidence: 0.2,
      metrics: {
        wordCount: words.length,
        sentenceCount: sentences.length,
        reason: "Transcript is too short for reliable classification."
      },
      explanation: "Short transcripts default to A1 with low confidence.",
      classifierVersion: CLASSIFIER_VERSION
    };
  }

  const uniqueWords = new Set(words);
  const syllableCount = words.reduce((total, word) => total + countSyllables(word), 0);
  const sentenceCount = Math.max(sentences.length, 1);
  const wordCount = words.length;
  const avgSentenceLength = wordCount / sentenceCount;
  const avgSyllablesPerWord = syllableCount / wordCount;
  const fleschKincaidGrade = Math.max(
    0,
    0.39 * avgSentenceLength + 11.8 * avgSyllablesPerWord - 15.59
  );
  const longWordRatio = words.filter((word) => word.length >= 8).length / wordCount;
  const advancedMarkerRatio = words.filter(isAdvancedMarker).length / wordCount;
  const uniqueWordRatio = uniqueWords.size / wordCount;
  const vocabularyDistribution = buildVocabularyDistribution(words);

  const indicators = [
    gradeToLevelIndex(fleschKincaidGrade),
    sentenceLengthToLevelIndex(avgSentenceLength),
    lexicalToLevelIndex(longWordRatio, advancedMarkerRatio, uniqueWordRatio),
    vocabularyToLevelIndex(vocabularyDistribution)
  ];

  const weights = [0.35, 0.2, 0.3, 0.15];
  const weightedScore = indicators.reduce((total, indicator, index) => total + indicator * weights[index], 0);
  const levelIndex = clamp(Math.round(weightedScore), 0, LEVELS.length - 1);
  const confidence = confidenceFromAgreement(indicators, wordCount);
  const level = LEVELS[levelIndex];

  return {
    level,
    confidence,
    metrics: {
      wordCount,
      sentenceCount,
      uniqueWordCount: uniqueWords.size,
      avgSentenceLength: round(avgSentenceLength),
      avgSyllablesPerWord: round(avgSyllablesPerWord),
      fleschKincaidGrade: round(fleschKincaidGrade),
      longWordRatio: round(longWordRatio),
      advancedMarkerRatio: round(advancedMarkerRatio),
      uniqueWordRatio: round(uniqueWordRatio),
      vocabularyDistribution,
      indicatorLevels: indicators.map((indicator) => LEVELS[indicator]),
      weightedScore: round(weightedScore)
    },
    explanation: buildExplanation(level, fleschKincaidGrade, avgSentenceLength, longWordRatio, advancedMarkerRatio),
    classifierVersion: CLASSIFIER_VERSION
  };
}

export function countWords(text) {
  return tokenizeWords(text).length;
}

function tokenizeWords(text) {
  return text
    .toLowerCase()
    .replace(/&[a-z]+;/g, " ")
    .match(/[a-z]+(?:'[a-z]+)?/g) || [];
}

function splitSentences(text) {
  const sentences = text
    .replace(/\s+/g, " ")
    .split(/[.!?]+/)
    .map((sentence) => sentence.trim())
    .filter(Boolean);

  return sentences.length ? sentences : [text.trim()].filter(Boolean);
}

function countSyllables(word) {
  const normalized = word.toLowerCase().replace(/(?:e|ed|es)$/u, "");
  const matches = normalized.match(/[aeiouy]+/g);
  return Math.max(matches?.length || 1, 1);
}

function isAdvancedMarker(word) {
  return (
    word.length >= 10 ||
    ADVANCED_SUFFIXES.some((suffix) => word.endsWith(suffix)) ||
    ["however", "therefore", "whereas", "nevertheless", "consequently"].includes(word)
  );
}

function buildVocabularyDistribution(words) {
  const distribution = Object.fromEntries(LEVELS.map((level) => [level, 0]));
  let matchedWords = 0;

  for (const word of words) {
    const level = WORD_LEVEL.get(word);
    if (!level) {
      continue;
    }

    distribution[level] += 1;
    matchedWords += 1;
  }

  return {
    ...distribution,
    matchedWords,
    matchedRatio: round(matchedWords / words.length)
  };
}

function gradeToLevelIndex(grade) {
  if (grade <= 2) return 0;
  if (grade <= 4) return 1;
  if (grade <= 6) return 2;
  if (grade <= 8.5) return 3;
  if (grade <= 11) return 4;
  return 5;
}

function sentenceLengthToLevelIndex(avgSentenceLength) {
  if (avgSentenceLength <= 8) return 0;
  if (avgSentenceLength <= 12) return 1;
  if (avgSentenceLength <= 16) return 2;
  if (avgSentenceLength <= 22) return 3;
  if (avgSentenceLength <= 28) return 4;
  return 5;
}

function lexicalToLevelIndex(longWordRatio, advancedMarkerRatio, uniqueWordRatio) {
  const score = longWordRatio * 3.5 + advancedMarkerRatio * 8 + uniqueWordRatio * 0.8;

  if (score <= 0.45) return 0;
  if (score <= 0.65) return 1;
  if (score <= 0.85) return 2;
  if (score <= 1.05) return 3;
  if (score <= 1.25) return 4;
  return 5;
}

function vocabularyToLevelIndex(distribution) {
  if (distribution.matchedWords < 10 || distribution.matchedRatio < 0.08) {
    return 2;
  }

  const weighted = LEVELS.reduce((total, level, index) => total + distribution[level] * index, 0);
  return clamp(Math.round(weighted / distribution.matchedWords), 0, LEVELS.length - 1);
}

function confidenceFromAgreement(indicators, wordCount) {
  const mean = indicators.reduce((total, indicator) => total + indicator, 0) / indicators.length;
  const variance = indicators.reduce((total, indicator) => total + (indicator - mean) ** 2, 0) / indicators.length;
  const agreement = Math.max(0, 1 - Math.sqrt(variance) / 2.5);
  const lengthFactor = Math.min(1, wordCount / 350);

  return round(0.25 + agreement * 0.5 + lengthFactor * 0.25);
}

function buildExplanation(level, grade, avgSentenceLength, longWordRatio, advancedMarkerRatio) {
  return `Estimated ${level} from readability grade ${round(grade)}, average sentence length ${round(avgSentenceLength)}, long-word ratio ${round(longWordRatio)}, and advanced-marker ratio ${round(advancedMarkerRatio)}.`;
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function round(value) {
  return Number(value.toFixed(3));
}
