# Learn module content

The Learn module (`translator/pro/learn/`) reads plain JSON from `app/src/main/assets/learn/`.
Nothing is downloaded and no translation API is called at runtime.

```
assets/learn/
├── manifest.json        launch courses + languages that have meaning ("gloss") files
├── concepts.json        the shared A1 syllabus: 12 units × 5 lessons × 6 items (360 ids, English)
├── text/<lang>.json     every concept id → {"t": text, "alt": [other correct answers]}
└── tips/<explain>/<course>.json   grammar tips for <course>, written in language <explain>
```

The same syllabus is used for every course. A concept's text in the **course** language is what
gets taught. Its text in the **learner's app language** is the meaning shown next to it. So one
`text/<lang>.json` per language serves both purposes.

Check every change with:

```
python tools/learn-content/validate.py          # all files
python tools/learn-content/validate.py es pt    # only these languages
```

## text/<lang>.json

```json
{
 "hello": {"t": "Hola", "alt": ["¡Hola!"]},
 "water": {"t": "el agua", "alt": ["agua"]},
 "what_is_your_name": {"t": "¿Cómo te llamas?", "alt": ["¿Cómo se llama usted?", "¿Cuál es tu nombre?"]}
}
```

Every id in `concepts.json` must be present, with no extra ids. Only the fields `t` and `alt` are
allowed.

### "t": the one canonical answer

- Use the most natural, common, correct wording a beginner course would teach. Use a neutral
  standard variety:
  - Spanish: international/Latin-American-neutral, `tú` for informal, never `vosotros`.
  - Portuguese: Brazilian.
  - French: France.
  - German: Germany.
  - Italian: Italy.
  - English: US spelling and words.
- Follow the English form:
  - Sentences keep a capital letter and end punctuation, including `¿…?` / `¡…!` in Spanish.
  - Single words are lower-case, unless the language capitalises them (German nouns, English
    weekdays and months).
- **Nouns in the courses es, fr, de, it:** put the definite article in `t` (`el agua`,
  `la mesa`, `l'eau`, `der Tisch`, `il pane`, `lo zucchero`) and the bare noun in `alt`. This is
  how learners pick up gender. Plurals use the plural article (`los zapatos`).
- Concept notes in `concepts.json` decide ambiguous forms:
  - "male; female form in alt" → `t` is masculine, the feminine goes in `alt`.
  - "informal; formal in alt" → `t` uses tú / tu / du / tu, and the usted / vous / Sie / Lei
    version goes in `alt`.
  - "formal or informal imperative, give both" → one in `t`, the other in `alt`.
- Places are spelled the target language's way: "Soy de México.", "Vivo en Londres."
- Keep digits where the English has digits (25). Keep "euros".

### "alt": other answers that are also fully correct

- Include:
  - the bare noun without its article
  - gender and formality variants
  - common contractions and synonyms a learner might really type
  - for English: I'm / it's / don't, check / bill, restroom / bathroom, lift / elevator
- Do not add variants that differ only in capitalisation, punctuation or spacing. The checker
  ignores those.
- Do not add versions without accents. The checker accepts a missing accent and points it out.
- At most about 6 entries. Omit `alt` (or use `[]`) when there is nothing to add.
- Never put a near-miss, a partial answer or a different meaning in `alt`.

### Gloss-only languages (pt, ru, hi, ko)

These languages are not taught yet. Their file only shows meanings to learners whose app language
it is:
- `t` must be a natural translation.
- Articles are not required.
- `alt` is optional.

`text/en.json` is both a gloss file and the English course:
- `t` is the concept's `en` text exactly.
- `alt` holds the other correct English answers.

## tips/<explain>/<course>.json

```json
[
 {"unit": 2, "title": "Saying where you are from", "body": "Use \"soy de\" + a place…", "examples": ["i_am_from_mexico", "where_are_you_from"]}
]
```

- **Number and placement:** 6–7 tips per course, at most one per unit. Each tip is shown before
  the first lesson of its unit, and again from the unit card.
- **`title` and `body`:**
  - Title: at most 32 characters.
  - Body: at most 220 characters.
  - Write simply for a complete beginner. "noun" and "verb" are the only grammar words allowed.
- **`examples`:** 2–4 concept ids from that unit or an earlier one. The app shows each one in the
  course language with its meaning in the learner's language, plus a speaker button.
- **Explanation language:**
  - `tips/en/<course>.json` explains each course in English. This is also the fallback for every
    app language.
  - `tips/<lang>/en.json` explains the English course in the learner's language. It is a
    translation of `tips/en/en.json`: same units, same examples.

Everything in these files is original course text written for this app. It contains no
third-party sentence data, so no attribution is needed. If sentences are ever taken from a source
such as Tatoeba (CC BY 2.0 FR), add a credit to the Learn settings screen.
