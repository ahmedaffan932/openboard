#!/usr/bin/env python3
"""Builds app/src/main/assets/wordgame_alternates.json for the Fun Words game.

Each puzzle's tiles are the shuffled letters of one stored word, and the game used to accept only
that word. Many puzzles also spell other real words ("art" -> "rat", "tar"; "low" -> "owl"), and
players who found those were told they were wrong. This script lists, for every puzzle word, the
other common English words made of exactly the same letters; the game accepts those too.

Inputs (both in the repo):
  - the puzzle list: translated-phrases/sorted_word.json inside
    app/src/main/assets/translated-phrases.zip
  - the English word list the keyboard dictionary is built from:
    dictionaries/en_US_wordlist.combined.gz

Re-run it whenever sorted_word.json changes:
    python tools/wordgame/make_alternates.py
"""

import gzip
import io
import json
import os
import re
import zipfile

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PUZZLE_ZIP = os.path.join(REPO, "app", "src", "main", "assets", "translated-phrases.zip")
PUZZLE_ENTRY = "translated-phrases/sorted_word.json"
WORDLIST = os.path.join(REPO, "dictionaries", "en_US_wordlist.combined.gz")
OUTPUT = os.path.join(REPO, "app", "src", "main", "assets", "wordgame_alternates.json")

# Only reasonably common words count. The word list's f= value runs 0-255; obscure entries
# ("ait", "tav") sit well below this, everyday ones ("rat" 106, "owl" 97, "tar" 98) above it.
MIN_FREQUENCY = 60
# Entries with any of these flags never count as answers.
EXCLUDED_FLAGS = {"offensive", "nonword", "abbreviation", "medical", "babytalk", "drugs", "hand"}
WORD_LINE = re.compile(r"^ word=([^,]+),f=(\d+),flags=([^,]*)")


def signature(word):
    return "".join(sorted(word))


def load_dictionary():
    """Lowercase a-z words, keyed by their sorted letters."""
    by_signature = {}
    with gzip.open(WORDLIST, "rt", encoding="utf-8") as f:
        for line in f:
            m = WORD_LINE.match(line)
            if not m:
                continue
            word, freq, flags = m.group(1), int(m.group(2)), m.group(3)
            # Capitalised entries are names and places; skip them, and anything not plain a-z.
            if not re.fullmatch(r"[a-z]{3,}", word):
                continue
            if freq < MIN_FREQUENCY:
                continue
            if set(flags.split(":")) & EXCLUDED_FLAGS:
                continue
            by_signature.setdefault(signature(word), set()).add(word)
    return by_signature


def load_puzzle_words():
    with zipfile.ZipFile(PUZZLE_ZIP) as z:
        data = json.load(io.TextIOWrapper(z.open(PUZZLE_ENTRY), encoding="utf-8"))
    return [entry["word"] for entry in data["data"]]


def main():
    by_signature = load_dictionary()
    alternates = {}
    for word in load_puzzle_words():
        target = word.lower()
        others = sorted(by_signature.get(signature(target), set()) - {target})
        if others:
            alternates[target] = others
    with open(OUTPUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(alternates, f, ensure_ascii=False, indent=1, sort_keys=True)
        f.write("\n")
    total = sum(len(v) for v in alternates.values())
    print(f"{len(alternates)} puzzle words have {total} other accepted words -> {OUTPUT}")


if __name__ == "__main__":
    main()
