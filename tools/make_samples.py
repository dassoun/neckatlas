"""Prépare les échantillons de guitare et de basse de l'appli (le son des notes jouées).

Sources, deux banques libres de droits (CC0, aucune attribution obligatoire) à télécharger à part :
  - guitare : « Emilyguitar » de Karoryfer Samples (D. Smolken), github.com/sfzinstruments/karoryfer.emilyguitar,
              dossier `notes/` (24 bits, 44,1 kHz, une note toutes les tierces mineures, 3 variantes par note) ;
  - basse   : « Finger Bass YR » de FreePats (Andrea Biasior, Yamaha RBX aux doigts), freepats.zenvoid.org,
              dossier `samples/finger/` (12 notes, de Mi1 à Ré#2).

Usage : python -I tools/make_samples.py <dossier notes d'Emilyguitar> <dossier samples/finger de la basse>

Pour chaque échantillon : retire la composante continue, mesure sa hauteur réelle et le ramène à la note exacte (les
instruments enregistrés sont toujours un peu faux, de quelques cents), le convertit en 16 bits mono à 22,05 kHz, le
normalise (crête à 0,9, comme le synthétiseur) et l'écrit en PCM brut. Écrit aussi `manifest.txt`, que l'appli lit pour
savoir quelles notes existent. Sortie : shared/src/commonMain/composeResources/files/samples/.

À l'écoute, la lecture à bord (`SampleBank`) accorde les notes intermédiaires en lisant l'échantillon le plus proche plus
vite ou plus lentement ; ce script ne fait que préparer la matière.
"""
import math
import struct
import sys
import wave
from operator import mul
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
OUT = PROJECT / "shared" / "src" / "commonMain" / "composeResources" / "files" / "samples"

IN_RATE = 44_100
OUT_RATE = 22_050  # la prise de son est sombre ; un haut-parleur de téléphone ne rend rien au-dessus de 11 kHz
PEAK = 0.9
TAIL_SECONDS = 0.1  # fondu de fin, pour qu'un échantillon coupé ne claque pas

# Numéro MIDI de chaque échantillon, avec son nom dans la banque.
GUITAR_NOTES = {37: "db2", 40: "e2", 42: "gb2", 45: "a2", 48: "c3", 51: "eb3", 54: "gb3", 57: "a3", 60: "c4",
                63: "eb4", 66: "gb4", 69: "a4", 72: "c5", 75: "eb5", 78: "gb5", 81: "a5", 84: "c6", 86: "d6"}
GUITAR_VARIANTS = 3
# Durée gardée de chaque échantillon. L'appli en joue NOTE_SECONDS (1,5 s, audio/PluckSynth.kt) ; il faut au moins
# NOTE_SECONDS x 1,12 pour les notes accordées 2 demi-tons plus haut. On garde le double : rallonger les notes ne demande
# alors pas de régénérer. Une fois la durée arrêtée, 1,8 s pour la guitare et 3,5 s pour la basse (voir ci-dessous) ôteraient
# environ 4 Mo.
GUITAR_SECONDS = 3.0
BASS_NOTES = {28: "E", 29: "F", 30: "F#", 31: "G", 32: "G#", 33: "A", 34: "A#", 35: "B", 36: "C", 37: "C#", 38: "D", 39: "D#"}
# La basse n'a qu'une octave : on garde toute la note, car les aiguës la lisent bien plus vite (jusqu'à 2,5 fois, case 24 de
# la basse 4 cordes). Avec des notes de 1,5 s, 3,5 s suffiraient.
BASS_SECONDS = 6.5


def sinc(x):
    return 1.0 if x == 0 else math.sin(math.pi * x) / (math.pi * x)


def read_wav(path):
    with wave.open(str(path)) as w:
        assert w.getnchannels() == 1 and w.getframerate() == IN_RATE and w.getsampwidth() == 3, path
        raw = w.readframes(w.getnframes())
    return [int.from_bytes(raw[i:i + 3], "little", signed=True) / 8388608.0 for i in range(0, len(raw), 3)]


def measure_hz(x, rate, expected_hz, start_seconds):
    """Hauteur d'une note par autocorrélation, avec interpolation parabolique (même méthode que les tests de l'appli)."""
    period = rate / expected_hz
    start = int(start_seconds * rate)
    window = max(int(8192 * rate / IN_RATE), int(period * 8))
    seg = x[start:start + window]
    if len(seg) < window:
        return None

    def r(lag):
        return sum(map(mul, seg[:len(seg) - lag], seg[lag:])) / (len(seg) - lag)

    lo, hi = int(period * 0.8), int(period * 1.2) + 1
    best = max(range(lo, hi), key=r)
    a, b, c = r(best - 1), r(best), r(best + 1)
    return rate / (best + 0.5 * (a - c) / (a - 2 * b + c))


def cents(measured, expected):
    return 1200 * math.log2(measured / expected)


def measured_error_cents(x, rate, midi):
    expected = 440.0 * 2 ** ((midi - 69) / 12)
    values = [m for m in (measure_hz(x, rate, expected, s) for s in (0.25, 0.6)) if m]
    return [cents(m, expected) for m in values]


# Rééchantillonnage par sinus cardinal fenêtré (Lanczos), avec une table de noyaux pour ne pas recalculer des sinus.
LOBES = 8
PHASES = 512


def resample(x, stride, n_out):
    """Lit `x` avec un pas de `stride` échantillons d'entrée par échantillon de sortie (pas > 1 : on filtre avant)."""
    scale = max(1.0, stride)
    half = int(math.ceil(LOBES * scale))
    table = []
    for phase in range(PHASES + 1):
        frac = phase / PHASES
        weights = []
        for j in range(-half + 1, half + 1):
            d = (j - frac) / scale
            weights.append(0.0 if abs(d) >= LOBES else sinc(d) * sinc(d / LOBES))
        total = sum(weights)
        table.append([w / total for w in weights])
    padded = [0.0] * half + x + [0.0] * (half + 1)
    out = []
    for i in range(n_out):
        p = i * stride
        n = int(p)
        start = n - half + 1 + half
        out.append(sum(map(mul, table[int(round((p - n) * PHASES))], padded[start:start + 2 * half])))
    return out


def prepare(path, midi, seconds):
    """Un échantillon prêt à être stocké : accordé, 22,05 kHz, centré, normalisé, fondu de fin."""
    x = read_wav(path)
    before = measured_error_cents(x, IN_RATE, midi)
    correction = sum(before) / len(before) if before else 0.0
    if abs(correction) > 30:  # une mesure aussi fausse est plus probablement une erreur de mesure qu'une note fausse
        print(f"  ! {path.name}: écart mesuré {correction:+.1f} cents, non corrigé")
        correction = 0.0
    stride = IN_RATE / OUT_RATE * 2 ** (-correction / 1200)
    n_out = min(int(seconds * OUT_RATE), int((len(x) - 1) / stride))
    y = resample(x, stride, n_out)
    mean = sum(y) / len(y)
    y = [v - mean for v in y]
    scale = PEAK / max(abs(v) for v in y)
    tail = int(TAIL_SECONDS * OUT_RATE)
    y = [v * scale * min(1.0, (len(y) - 1 - i) / tail) for i, v in enumerate(y)]
    after = measured_error_cents(y, OUT_RATE, midi)
    return y, before, after


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    guitar_dir, bass_dir = Path(sys.argv[1]), Path(sys.argv[2])
    OUT.mkdir(parents=True, exist_ok=True)
    for old in list(OUT.glob("*.pcm")) + list(OUT.glob("manifest.txt")):
        old.unlink()

    manifest = ["# Généré par tools/make_samples.py : ne pas modifier à la main.",
                "# rate = fréquence d'échantillonnage ; puis une ligne par note : instrument, numéro MIDI, nombre de variantes.",
                f"rate {OUT_RATE}"]
    jobs = []
    for midi, name in GUITAR_NOTES.items():
        manifest.append(f"guitar {midi} {GUITAR_VARIANTS}")
        jobs += [("guitar", midi, v, guitar_dir / f"{name}_mf_rr{v}.wav", GUITAR_SECONDS) for v in range(1, GUITAR_VARIANTS + 1)]
    for midi, name in BASS_NOTES.items():
        manifest.append(f"bass {midi} 1")
        jobs.append(("bass", midi, 1, bass_dir / f"{name}.wav", BASS_SECONDS))

    worst_after = 0.0
    total_bytes = 0
    for kind, midi, variant, path, seconds in jobs:
        y, before, after = prepare(path, midi, seconds)
        data = struct.pack("<%dh" % len(y), *[int(round(max(-1.0, min(1.0, v)) * 32767)) for v in y])
        (OUT / f"{kind}_{midi}_{variant}.pcm").write_bytes(data)
        total_bytes += len(data)
        worst_after = max([worst_after] + [abs(a) for a in after])
        print(f"{kind:6s} {midi:3d} v{variant}  {len(y) / OUT_RATE:4.1f} s  "
              f"avant {' '.join(f'{b:+5.1f}' for b in before):>12s} cents   après {' '.join(f'{a:+5.1f}' for a in after):>12s}")
    (OUT / "manifest.txt").write_text("\n".join(manifest) + "\n", encoding="utf-8")
    print(f"\n{len(jobs)} échantillons, {total_bytes / 1e6:.1f} Mo ; pire écart après correction : {worst_after:.1f} cents")


if __name__ == "__main__":
    main()
