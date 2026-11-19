#!/usr/bin/env python3
"""One-off data fix for the phrasebook asset (app/src/main/assets/translated-phrases.zip).

In translated-phrases/bn.json, Problems > Authority has two phrases under the same key "ভি". JSON
keeps only the last one, so Bengali had 9 phrases there against 10 in every other language, and
because the app pairs phrases by position, every Bengali phrase from row 5 on sat next to the
wrong English one (and the last row had no Bengali at all).

This renames the two keys to unique ones, rewrites only that entry of the zip (all other entries are
copied byte for byte with their original compression), and checks the result.

    python tools/phrasebook/fix_bn_duplicate_key.py
"""

import json
import os
import shutil
import tempfile
import zipfile

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ZIP_PATH = os.path.join(REPO, "app", "src", "main", "assets", "translated-phrases.zip")
ENTRY = "translated-phrases/bn.json"
DUPLICATE = '"ভি":'  # "ভি":
REPLACEMENTS = ['"ভি 6":', '"ভি 7":']  # "ভি 6", "ভি 7"


def fixed_bn(text):
    if text.count(DUPLICATE) == 0:
        return None  # Already fixed.
    assert text.count(DUPLICATE) == len(REPLACEMENTS), text.count(DUPLICATE)
    for replacement in REPLACEMENTS:
        text = text.replace(DUPLICATE, replacement, 1)
    return text


def check_alignment(zf):
    en = json.loads(zf.read("translated-phrases/en.json").decode("utf-8"))
    bn = json.loads(zf.read(ENTRY).decode("utf-8"))
    assert len(en) == len(bn)
    for en_cat, bn_cat in zip(en.values(), bn.values()):
        assert len(en_cat) == len(bn_cat)
        for en_sub, bn_sub in zip(en_cat.values(), bn_cat.values()):
            assert len(en_sub) == len(bn_sub), (len(en_sub), len(bn_sub))


def main():
    with zipfile.ZipFile(ZIP_PATH) as src:
        text = src.read(ENTRY).decode("utf-8")
        new_text = fixed_bn(text)
        if new_text is None:
            print("bn.json has no duplicate key; nothing to do")
            check_alignment(src)
            return
        fd, tmp = tempfile.mkstemp(suffix=".zip")
        os.close(fd)
        with zipfile.ZipFile(tmp, "w") as dst:
            for info in src.infolist():
                if info.filename == ENTRY:
                    dst.writestr(info, new_text.encode("utf-8"), compress_type=info.compress_type)
                else:
                    dst.writestr(info, src.read(info.filename), compress_type=info.compress_type)
    with zipfile.ZipFile(tmp) as check:
        check_alignment(check)
    shutil.move(tmp, ZIP_PATH)
    print("Fixed the duplicate key in", ENTRY)


if __name__ == "__main__":
    main()
