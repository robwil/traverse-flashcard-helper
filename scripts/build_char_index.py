#!/usr/bin/env python3
"""
build_char_index.py

This script takes in a list of characters and a SUBTLEX-CH word frequency file
and builds a dictionary of characters to the top N (default 3) words in the file.
This is then embedded in our tampermonkey script.

Inputs:
 - input_chars.csv   (one simplified character per line, UTF-8)
 - subtlex_file      (path to SUBTLEX-CH wordfreq XLSX file)
Outputs:
 - char_index.json   (minified JSON, keys = single characters, values = list of up to 3 words in descending frequency order)

Usage examples:
  python build_char_index.py -c input_chars.csv -s SUBTLEX-CH-WF/SUBTLEX-CH-WF.xlsx -o char_index.json

Notes:
 - If your word file already contains a frequency column named 'Freq' or 'freq' or 'Frequency', the script will use it.
 - If word column is named differently, pass --wordcol and --freqcol.
"""

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path

try:
    import pandas as pd
except ImportError:
    print("This script requires pandas. Install with: pip install pandas", file=sys.stderr)
    sys.exit(1)

# Optional: opencc to convert traditional -> simplified (if your SUBTLEX words are in traditional)
try:
    from opencc import OpenCC
    opencc = OpenCC('t2s')  # traditional to simplified
except Exception:
    opencc = None

def read_wordfreq(path, word_col_guesses=('Word','word','wordform','WordForm','w','token'),
                  freq_col_guesses=('WCount','Freq','freq','Frequency','frequency','FreqCount','Count')):
    """
    Read a word-frequency XLSX file.
    Returns a DataFrame with at least two columns: word, freq
    """
    p = Path(path)
    if not p.exists():
        raise FileNotFoundError(f"{path} not found")

    # SUBTLEX-CH-WF has 2 metadata rows before the actual headers, so skip them
    df = pd.read_excel(p, skiprows=2)

    # Normalize column names
    cols = {c: c.strip() for c in df.columns}
    df.rename(columns=cols, inplace=True)

    # find word column
    word_col = None
    for c in word_col_guesses:
        if c in df.columns:
            word_col = c
            break
    # fallback: first column
    if word_col is None:
        word_col = df.columns[0]

    # find freq column
    freq_col = None
    for c in freq_col_guesses:
        if c in df.columns:
            freq_col = c
            break
    # fallback: second column if it looks numeric
    if freq_col is None:
        if len(df.columns) > 1 and pd.api.types.is_numeric_dtype(df.iloc[:,1]):
            freq_col = df.columns[1]
        else:
            # if no numeric column is found, set all to 1
            df['__freq__'] = 1
            freq_col = '__freq__'

    # ensure columns exist
    df = df[[word_col, freq_col]].copy()
    df.columns = ['word', 'freq']

    # try coerce freq to int
    df['freq'] = pd.to_numeric(df['freq'], errors='coerce').fillna(0).astype(int)

    # trim whitespace
    df['word'] = df['word'].astype(str).str.strip()

    return df

def build_index(chars, df_words, top_n=3, convert_to_simplified=False):
    """
    Build dictionary: char -> list of words sorted by freq desc, up to top_n
    chars: iterable of single characters (strings)
    df_words: DataFrame with columns ['word','freq']
    """
    # optional conversion
    if convert_to_simplified:
        if opencc is None:
            raise RuntimeError("opencc required for conversion to simplified but not installed. pip install opencc-python-reimplemented")
        # convert words column
        df_words['word'] = df_words['word'].apply(lambda s: opencc.convert(s))

    # Create mapping from char -> dict(word -> freq (max if multiple entries))
    bychar = defaultdict(lambda: defaultdict(int))

    # We'll iterate the word list top-down (sorted by freq) which is often faster for early truncation
    df_sorted = df_words.sort_values('freq', ascending=False)

    # Create a set for quick lookup of target characters
    target_chars = set(chars)

    for _, row in df_sorted.iterrows():
        w = row['word']
        f = int(row['freq'])
        if not w: continue
        # speed: if none of chars in target set intersect with word, skip
        # but faster check is to check for any character that is in word and in target_chars
        # we'll iterate unique set of characters in w
        for ch in set(w):
            if ch in target_chars:
                # record max frequency for this word (if duplicate from different rows)
                bychar[ch][w] = max(bychar[ch].get(w, 0), f)

    # Now produce top-N lists
    out = {}
    for ch in chars:
        words = bychar.get(ch, {})
        if not words:
            out[ch] = []
        else:
            sorted_words = sorted(words.items(), key=lambda t: -t[1])[:top_n]
            out[ch] = [w for w, freq in sorted_words]

    return out

def main():
    ap = argparse.ArgumentParser(description="Build minified char -> top-N word frequency JSON from SUBTLEX-style wordlist")
    ap.add_argument('-c','--chars', required=True, help="input_chars.csv (one character per line, simplified)")
    ap.add_argument('-s','--subtlex', required=True, help="SUBTLEX word frequency XLSX file.")
    ap.add_argument('-o','--out', default='char_index.json', help="output json file")
    ap.add_argument('-n','--topn', type=int, default=3, help="top N words per character")
    ap.add_argument('--convert', action='store_true', help="convert input words to simplified with opencc (t2s). Requires opencc.")
    args = ap.parse_args()

    # read chars
    chars_path = Path(args.chars)
    if not chars_path.exists():
        print(f"Chars file not found: {chars_path}", file=sys.stderr)
        sys.exit(2)
    with open(chars_path, 'r', encoding='utf-8') as f:
        chars = [line.strip() for line in f if line.strip()]
    # Validate single-character lines
    bad = [c for c in chars if len(c) == 0 or len(c) > 1]
    if bad:
        print(f"Warning: some lines in {chars_path} are not single characters (they will be ignored): {bad[:10]}", file=sys.stderr)
        chars = [c for c in chars if len(c) == 1]

    print(f"Reading word-frequency file: {args.subtlex} ...")
    df_words = read_wordfreq(args.subtlex)
    print(f"Total word rows read: {len(df_words):,}")

    # Build index
    print(f"Building index for {len(chars):,} characters, top {args.topn} each ...")
    index = build_index(chars, df_words, top_n=args.topn, convert_to_simplified=args.convert)

    # Write minified JSON (ensure_ascii=False to keep Chinese)
    out_path = Path(args.out)
    with open(out_path, 'w', encoding='utf-8') as fo:
        json.dump(index, fo, ensure_ascii=False, separators=(',',':'))

    print(f"Wrote {out_path} (minified). Characters with zero hits will have empty lists.")

if __name__ == '__main__':
    main()
