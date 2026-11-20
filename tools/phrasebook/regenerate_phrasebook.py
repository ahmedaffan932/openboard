#!/usr/bin/env python3
"""Regenerates the phrasebook translations in app/src/main/assets/translated-phrases.zip.

The shipped translations were machine-translated from English that had typos ("soar throat",
"antibodies", "Ninteen") and no context ("Right" came out as "correct" in many languages), and
some languages had words left in English. This script:

  1. fixes the English source (ENGLISH_FIXES below) and gives ambiguous phrases context
     ("Turn right", "boarding gate");
  2. puts each group's phrases in their natural order (the files had them sorted alphabetically,
     so days ran Friday, Thursday, Monday...);
  3. translates every category, group and phrase into each language with Google Translate's free
     web endpoint (the one the app itself uses), one request per language;
  4. checks the results (same structure as English, nothing empty, no duplicate names, no English
     left in non-Latin scripts) and writes every language file back into the zip. Other zip
     entries (sorted_word.json) are copied unchanged.

Phrase keys become "v1", "v2", ... in display order. The app pairs a phrase with its translation
by position, so every language must have exactly the English structure; the unit test
PhrasebookAssetTest checks that.

    python tools/phrasebook/regenerate_phrasebook.py            # all languages
    python tools/phrasebook/regenerate_phrasebook.py de fr      # only these (English always)

Translations are cached in the system temp folder, so a re-run only fetches what is missing.
"""

import io
import json
import os
import re
import sys
import tempfile
import time
import urllib.parse
import urllib.request
import zipfile

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ZIP_PATH = os.path.join(REPO, "app", "src", "main", "assets", "translated-phrases.zip")
PREFIX = "translated-phrases/"
CACHE = os.path.join(tempfile.gettempdir(), "phrasebook_translation_cache.json")
ENDPOINT = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl={tl}&dt=t"

# App language code (file name) -> Google web code.
WEB_CODES = {"he": "iw", "zh": "zh-CN"}

# Scripts that should contain no English words after translation.
NON_LATIN = {"ar", "be", "bg", "bn", "el", "fa", "gu", "he", "hi", "ja", "ka", "kn", "ko",
             "mk", "mr", "ru", "ta", "te", "th", "uk", "ur", "zh"}
# English allowed inside a non-Latin translation (abbreviations, the blank).
ALLOWED_LATIN = {"ATM", "OK"}

# Exact English fixes: typos, grammar, and context for words that are ambiguous on their own.
ENGLISH_FIXES = {
    # Category and group names.
    "At The Store": "At the Store",
    "At the bar": "At the Bar",
    "At the resturaunt": "At the Restaurant",
    "Make A Reservation": "Make a Reservation",
    # Phrases.
    "I want to return this product?": "I want to return this product.",
    "Can I pay with Card?": "Can I pay by card?",
    "Where are the vegitables?": "Where are the vegetables?",
    "My room needs to be clean.": "My room needs to be cleaned.",
    "Can I have a room with ___ view?": "Can I have a room with a ___ view?",
    "Is lunch inlcuded?": "Is lunch included?",
    "Is there a resturaunt?": "Is there a restaurant?",
    "Do you have free rooms?": "Do you have any rooms available?",
    "I would like __ rooms.": "I would like ___ rooms.",
    "What white wine do you sggest?": "What white wine do you suggest?",
    "May I see the menu.": "May I see the menu?",
    "What apperitive do you suggest?": "What aperitif do you suggest?",
    "I am allergic to shell fish.": "I am allergic to shellfish.",
    "I am allaergic to wheat.": "I am allergic to wheat.",
    "I am a vegitarian.": "I am a vegetarian.",
    "I dont eat meat.": "I don't eat meat.",
    "I dont eat pork.": "I don't eat pork.",
    "I dont eat ___.": "I don't eat ___.",
    "Thankyou": "Thank you",
    "Straight Ahead": "Go straight ahead",
    "Left": "Turn left",
    "Right": "Turn right",
    "Stop Sign": "Stop sign",
    "Traffic Light": "Traffic light",
    "I need help": "I need help.",
    "My name is .": "My name is ___.",
    "I don't speak ___": "I don't speak ___.",
    "Where is the ATM.": "Where is the ATM?",
    "I am allergic to antibodies.": "I am allergic to antibiotics.",
    "I need to got to a pharmacy.": "I need to go to a pharmacy.",
    "I have headache.": "I have a headache.",
    "I have a soar throat.": "I have a sore throat.",
    "Am I under Arrest?": "Am I under arrest?",
    "It was misunderstanding.": "It was a misunderstanding.",
    "Its an emergency.": "It's an emergency.",
    "Ninteen": "Nineteen",
    "Where is the baggage drop claim?": "Where is the baggage claim?",
    "Where is the gate __?": "Where is boarding gate ___?",
    "Which gate?": "Which boarding gate?",
    "Where are the Arrivals?": "Where are the arrivals?",
    "Where are the Departures?": "Where are the departures?",
    "I need __ tickets please.": "I need ___ tickets, please.",
    "Do I need visa?": "Do I need a visa?",
    "Where is airport?": "Where is the airport?",
    "Where is train station?": "Where is the train station?",
    "Where is a nice coffee place?": "Where is a nice café?",
    "Where is the __ museum?": "Where is the ___ museum?",
    "Where should I Get off?": "Where should I get off?",
    "Where can I Rent a car?": "Where can I rent a car?",
}
# Sent to the translator instead of the displayed English, where a short name alone is ambiguous
# ("At the Bar" came back as "at the counter", "Days" as "number of days").
TRANSLATION_HINTS = {
    "At the Bar": "In the bar",
    "Days": "Days of the week",
    "Months": "Months of the year",
    "Time": "Times of day",
}

# Groups whose original numbering was not a natural reading order.
GROUP_ORDER = {
    "Directions": ["Turn left", "Turn right", "Go straight ahead", "In ___ meters.",
                   "Traffic light", "Stop sign", "North", "South", "East", "West"],
}

# Languages where upper-casing the first letter is wrong or meaningless (Turkish dotted i).
NO_CAPITALIZE = {"tr", "az"}

# "I am allergic to soya beans." appears twice in Restaurant > Allergies; the second becomes this.
DUPLICATE_ALLERGY_REPLACEMENT = "I am allergic to gluten."

NUMBER_WORDS = {"ten": 10}


def key_order(key):
    """Display position from the original key: "v7" -> 7, "ten" -> 10."""
    if key in NUMBER_WORDS:
        return NUMBER_WORDS[key]
    m = re.fullmatch(r"v(\d+)", key)
    return int(m.group(1)) if m else 10_000


def english_source(en):
    """[(category, [(group, [phrase, ...]), ...]), ...] with fixes applied and natural order."""
    result = []
    for category, groups in en.items():
        fixed_groups = []
        for group, phrases in groups.items():
            ordered = [text for _, text in sorted(phrases.items(), key=lambda kv: key_order(kv[0]))]
            fixed = []
            for text in ordered:
                text = ENGLISH_FIXES.get(text, text)
                if text in fixed and text == "I am allergic to soya beans.":
                    text = DUPLICATE_ALLERGY_REPLACEMENT
                fixed.append(text)
            assert len(set(fixed)) == len(fixed), (group, fixed)
            group_name = ENGLISH_FIXES.get(group, group)
            if group_name in GROUP_ORDER:
                assert sorted(GROUP_ORDER[group_name]) == sorted(fixed), group_name
                fixed = list(GROUP_ORDER[group_name])
            fixed_groups.append((group_name, fixed))
        result.append((ENGLISH_FIXES.get(category, category), fixed_groups))
    return result


def flatten(source):
    """Every string to translate, in a fixed order: categories, then groups, then phrases."""
    items = [category for category, _ in source]
    for _, groups in source:
        items.extend(group for group, _ in groups)
    for _, groups in source:
        for _, phrases in groups:
            items.extend(phrases)
    return items


def translate_batch(lines, language):
    body = urllib.parse.urlencode({"q": "\n".join(lines)}).encode("utf-8")
    url = ENDPOINT.format(tl=WEB_CODES.get(language, language))
    request = urllib.request.Request(url, data=body, headers={
        "User-Agent": "Mozilla/5.0",
        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
    })
    attempts = 6
    for attempt in range(attempts):
        try:
            raw = urllib.request.urlopen(request, timeout=30).read().decode("utf-8")
            data = json.loads(raw)
            text = "".join(seg[0] for seg in data[0] if seg and seg[0])
            return [line.strip() for line in text.split("\n")]
        except Exception as e:  # noqa: BLE001 - network: retry with backoff
            if attempt == attempts - 1:
                raise
            # "429 Too Many Requests" needs a real pause; other errors a short one.
            wait = 60 * (attempt + 1) if "429" in str(e) else 5 * (attempt + 1)
            print(f"  {language}: {e}; retrying in {wait}s", file=sys.stderr)
            time.sleep(wait)
    return []


def translate_all(items, language, cache):
    sent = [TRANSLATION_HINTS.get(s, s) for s in items]
    result = translate_sent(sent, language, cache)
    if language in NO_CAPITALIZE:
        return result
    # The endpoint lower-cases some short phrases ("en la tienda"); list entries start upper-case.
    return [t[:1].upper() + t[1:] if t[:1].islower() else t for t in result]


def translate_sent(items, language, cache):
    missing = list(dict.fromkeys(s for s in items if (language, s) not in cache))
    # A few separate requests keep each well inside the endpoint's size limits.
    chunk = 90
    for i in range(0, len(missing), chunk):
        part = missing[i:i + chunk]
        out = translate_batch(part, language)
        if len(out) != len(part):
            # Line structure lost: fall back to one request per string.
            out = [translate_batch([s], language)[0] for s in part]
        for s, t in zip(part, out):
            cache[(language, s)] = t
        time.sleep(4)
    return [cache[(language, s)] for s in items]


def unique(names):
    """Two groups translated to the same word would merge into one JSON key: number repeats."""
    seen = {}
    result = []
    for name in names:
        if name in seen:
            seen[name] += 1
            name = f"{name} ({seen[name]})"
        else:
            seen[name] = 1
        result.append(name)
    return result


def build_language(source, translated):
    """Rebuilds the language file from the flat translation list (same order as flatten)."""
    it = iter(translated)
    categories = unique([next(it) for _ in source])
    groups = [unique([next(it) for _ in groups]) for _, groups in source]
    data = {}
    for c_index, (_, groups_src) in enumerate(source):
        category = {}
        for g_index, (_, phrases) in enumerate(groups_src):
            category[groups[c_index][g_index]] = {
                f"v{i + 1}": next(it) for i in range(len(phrases))
            }
        data[categories[c_index]] = category
    return data


def check(language, source_items, translated):
    problems = []
    for src, out in zip(source_items, translated):
        if not out:
            problems.append(f"empty for {src!r}")
        elif language in NON_LATIN:
            words = [w for w in re.findall(r"[A-Za-z]{3,}", out) if w not in ALLOWED_LATIN]
            if words:
                problems.append(f"English left in {out!r} (for {src!r})")
    return problems


def load_cache():
    if not os.path.exists(CACHE):
        return {}
    with open(CACHE, encoding="utf-8") as f:
        return {(k.split("\t", 1)[0], k.split("\t", 1)[1]): v for k, v in json.load(f).items()}


def save_cache(cache):
    with open(CACHE, "w", encoding="utf-8") as f:
        json.dump({f"{lang}\t{src}": t for (lang, src), t in cache.items()}, f, ensure_ascii=False)


def main():
    wanted = set(sys.argv[1:])
    with zipfile.ZipFile(ZIP_PATH) as zf:
        entries = {info.filename: (info, zf.read(info.filename)) for info in zf.infolist()}
    en = json.loads(entries[PREFIX + "en.json"][1].decode("utf-8"))
    source = english_source(en)
    items = flatten(source)
    languages = sorted(
        name[len(PREFIX):-len(".json")] for name in entries
        if name.startswith(PREFIX) and name.endswith(".json")
        and not name.endswith("sorted_word.json") and not name.endswith("/en.json")
    )
    if wanted:
        languages = [lang for lang in languages if lang in wanted]

    cache = load_cache()
    new_files = {"en": build_language(source, items)}
    report = {}
    try:
        for language in languages:
            translated = translate_all(items, language, cache)
            save_cache(cache)
            report[language] = check(language, items, translated)
            new_files[language] = build_language(source, translated)
            print(f"{language}: {len(report[language])} warning(s)")
    finally:
        save_cache(cache)

    tmp = ZIP_PATH + ".tmp"
    with zipfile.ZipFile(tmp, "w") as out:
        for name, (info, data) in entries.items():
            language = name[len(PREFIX):-len(".json")] if name.endswith(".json") else None
            if language in new_files:
                data = json.dumps(new_files[language], ensure_ascii=False, indent=2).encode("utf-8")
            out.writestr(info, data, compress_type=info.compress_type)
    os.replace(tmp, ZIP_PATH)

    for language, problems in report.items():
        for problem in problems:
            print(f"  {language}: {problem}")
    print(f"Wrote {len(new_files)} language files into {ZIP_PATH}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
