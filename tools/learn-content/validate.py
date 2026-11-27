#!/usr/bin/env python3
"""Validates the Learn module content in app/src/main/assets/learn/.

    python tools/learn-content/validate.py            # every language file found
    python tools/learn-content/validate.py es fr      # only these text/tips files

Checks that each text/<lang>.json covers every concept in concepts.json with a non-empty "t",
that "alt" is a list of distinct strings, that tips files reference real units and concepts,
and that the launch courses (manifest.json) have complete text. Exits 1 on any error.
"""
import json
import os
import sys
import unicodedata

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'assets', 'learn')
ROOT = os.path.normpath(ROOT)


def load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def main(argv):
    errors, warnings = [], []
    concepts = load(os.path.join(ROOT, 'concepts.json'))
    ids = list(concepts['concepts'].keys())
    id_set = set(ids)
    unit_ids = {u['id'] for u in concepts['units']}
    for u in concepts['units']:
        for lesson in u['lessons']:
            for cid in lesson:
                if cid not in id_set:
                    errors.append(f'concepts.json: lesson item {cid} has no concept')

    manifest = load(os.path.join(ROOT, 'manifest.json'))
    courses = manifest['courses']

    text_dir = os.path.join(ROOT, 'text')
    langs = argv or sorted(f[:-5] for f in os.listdir(text_dir) if f.endswith('.json'))
    for lang in langs:
        path = os.path.join(text_dir, lang + '.json')
        if not os.path.exists(path):
            (errors if lang in courses else warnings).append(f'text/{lang}.json missing')
            continue
        try:
            data = load(path)
        except Exception as e:  # noqa: BLE001
            errors.append(f'text/{lang}.json: invalid JSON: {e}')
            continue
        missing = [c for c in ids if c not in data]
        extra = [c for c in data if c not in id_set]
        if missing:
            errors.append(f'text/{lang}.json: {len(missing)} missing, e.g. {missing[:8]}')
        if extra:
            errors.append(f'text/{lang}.json: unknown ids {extra[:8]}')
        for cid, entry in data.items():
            if not isinstance(entry, dict) or not isinstance(entry.get('t'), str) or not entry['t'].strip():
                errors.append(f'text/{lang}.json: {cid} has no "t"')
                continue
            t = entry['t']
            if t != t.strip() or '  ' in t:
                errors.append(f'text/{lang}.json: {cid} "t" has stray spaces')
            if unicodedata.normalize('NFC', t) != t:
                warnings.append(f'text/{lang}.json: {cid} "t" is not NFC')
            alt = entry.get('alt', [])
            if not isinstance(alt, list) or not all(isinstance(a, str) and a.strip() for a in alt):
                errors.append(f'text/{lang}.json: {cid} "alt" must be a list of non-empty strings')
                continue
            if t in alt:
                errors.append(f'text/{lang}.json: {cid} "alt" repeats "t"')
            if len(set(alt)) != len(alt):
                errors.append(f'text/{lang}.json: {cid} "alt" has duplicates')
            unknown = set(entry) - {'t', 'alt'}
            if unknown:
                errors.append(f'text/{lang}.json: {cid} unknown fields {sorted(unknown)}')

    tips_dir = os.path.join(ROOT, 'tips')
    if os.path.isdir(tips_dir):
        for explain in sorted(os.listdir(tips_dir)):
            if argv and explain not in argv and not any(f'{a}.json' in os.listdir(os.path.join(tips_dir, explain)) for a in argv):
                continue
            for fname in sorted(os.listdir(os.path.join(tips_dir, explain))):
                rel = f'tips/{explain}/{fname}'
                try:
                    tips = load(os.path.join(tips_dir, explain, fname))
                except Exception as e:  # noqa: BLE001
                    errors.append(f'{rel}: invalid JSON: {e}')
                    continue
                if fname[:-5] not in courses:
                    errors.append(f'{rel}: {fname[:-5]} is not a course')
                if not isinstance(tips, list):
                    errors.append(f'{rel}: must be a list')
                    continue
                seen_units = set()
                for tip in tips:
                    if tip.get('unit') not in unit_ids:
                        errors.append(f'{rel}: bad unit {tip.get("unit")}')
                    if tip.get('unit') in seen_units:
                        errors.append(f'{rel}: two tips for unit {tip.get("unit")}')
                    seen_units.add(tip.get('unit'))
                    for field in ('title', 'body'):
                        if not isinstance(tip.get(field), str) or not tip[field].strip():
                            errors.append(f'{rel}: unit {tip.get("unit")} has no {field}')
                    for ex in tip.get('examples', []):
                        if ex not in id_set:
                            errors.append(f'{rel}: unit {tip.get("unit")} example {ex} is not a concept')

    for w in warnings:
        print('warning:', w)
    for e in errors:
        print('ERROR:', e)
    print(f'{len(errors)} error(s), {len(warnings)} warning(s)')
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
