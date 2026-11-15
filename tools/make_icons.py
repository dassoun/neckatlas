"""Fabrique l'icône de lancement de Neck Atlas à partir de design/neckatlas-icon.webp.

L'illustration est un dessin à plat : un seul trait clair sur un fond uni. On n'en garde que la **forme** (la transparence du
dessin), et on lui donne les couleurs voulues : c'est ce qui permet d'essayer plusieurs couleurs sans retoucher l'image. On
obtient une vraie icône adaptative, nette à toutes les tailles, plutôt qu'une image posée telle quelle :
  - le dessin devient le calque de premier plan, de la couleur de l'encre (`--ink`) ;
  - le fond devient une couleur unie (`--background`).

Produit :
  - androidApp/src/main/res/mipmap-{mdpi..xxxhdpi}/ic_launcher_foreground.webp : le dessin sur fond transparent, cadré dans la
    zone visible d'un calque de 108 dp. Il sert aussi de calque monochrome (icônes thématiques d'Android 13+, où le système
    n'utilise que la transparence et colore le dessin lui-même) ;
  - androidApp/src/main/res/values/ic_launcher_background.xml : la couleur de fond ;
  - design/neckatlas-icon-512.png : l'icône de la fiche Google Play (512 x 512, plein cadre, fond uni, sans transparence).
Supprime les anciens calques d'arrière-plan en image (`ic_launcher_background.webp`), devenus inutiles.
Le fichier mipmap-anydpi/ic_launcher.xml, qui assemble le tout, est écrit à la main.

Usage : python -I tools/make_icons.py [--background #RRGGBB] [--ink #RRGGBB] [--preview planche.png] [--variants planche.png]
  --background, --ink : les couleurs de l'icône (par défaut celles de BACKGROUND et INK ci-dessous).
  --preview  : écrit aussi une planche de contrôle (rond, carré arrondi, carré, thématique) pour vérifier le cadrage.
  --variants : écrit une planche qui compare les variantes de VARIANTS sur fond sombre et sur fond clair, sans rien produire
               d'autre (ni ressources, ni couleur de fond) : pour choisir avant de changer l'icône.
"""
import statistics
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

PROJECT = Path(__file__).resolve().parent.parent
SRC = PROJECT / "design" / "neckatlas-icon.webp"
RES = PROJECT / "androidApp" / "src" / "main" / "res"
PLAY_STORE = PROJECT / "design" / "neckatlas-icon-512.png"

# Les couleurs de l'icône : le turquoise de l'anneau du logo Sensitive Mobility en fond, et son bleu nuit pour le dessin. Elles
# sont celles de la variante 3 de VARIANTS, choisie parmi cinq.
BACKGROUND = "#2EE6D0"
INK = "#08111C"

# Les variantes comparées par `--variants` : (nom, fond, encre).
VARIANTS = [
    ("1  Nuit et turquoise", "#0A1A2E", "#2EE6D0"),
    ("2  Nuit et blanc", "#0A1A2E", "#FFFFFF"),
    ("3  Turquoise et nuit", "#2EE6D0", "#08111C"),
    ("4  Bleu et blanc", "#2F86F5", "#FFFFFF"),
    ("5  Violet (avant)", "#660296", "#FFFFFF"),
]

LAYER_DP = 108  # taille d'un calque d'icône adaptative
VISIBLE_DP = 72  # zone toujours visible, au centre du calque ; le masque du lanceur la découpe (rond, carré arrondi…)
ART_DP = 68  # largeur de l'image d'origine une fois posée sur le calque (voir `fit_note` ci-dessous)
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

# Le fond de l'image n'est pas parfaitement uni (un peu de bruit de compression) : sur le canal vert, où le fond est presque
# noir et le trait plein, tout ce qui est sous NOISE_MARGIN au-dessus du fond est de la transparence, et tout ce qui dépasse
# FULL_LEVEL est du trait plein ; entre les deux, c'est le bord adouci du dessin.
NOISE_MARGIN = 4
FULL_LEVEL = 250


def rgb(hex_color: str) -> tuple[int, int, int]:
    h = hex_color.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


class Art:
    """L'illustration réduite à sa forme : la transparence du dessin, et son emprise."""

    def __init__(self, source: Image.Image):
        image = source.convert("RGB")
        w, h = image.size
        px = image.load()
        corners = [(0, 0), (w - 80, h - 80), (0, h - 80), (w - 80, 0)]
        patch = [px[x, y] for cx, cy in corners for x in range(cx, cx + 80, 4) for y in range(cy, cy + 80, 4)]
        background_green = int(statistics.median(p[1] for p in patch))
        low = background_green + NOISE_MARGIN
        self.alpha = image.getchannel("G").point(
            lambda v: 0 if v <= low else 255 if v >= FULL_LEVEL else round((v - low) * 255 / (FULL_LEVEL - low))
        )
        # Emprise du dessin, pour le centrer sur le calque même si l'image d'origine ne l'est pas tout à fait.
        self.bbox = self.alpha.point(lambda v: 255 if v > 40 else 0).getbbox()


def foreground(art: Art, layer_px: int, ink: str) -> Image.Image:
    """Le dessin, de la couleur [ink], sur transparent, cadré au centre d'un calque de `layer_px` pixels de côté."""
    scale = layer_px * ART_DP / LAYER_DP / art.alpha.width
    size = round(art.alpha.width * scale)
    scaled = art.alpha.resize((size, size), Image.LANCZOS)
    center_x = (art.bbox[0] + art.bbox[2]) / 2 * scale
    center_y = (art.bbox[1] + art.bbox[3]) / 2 * scale
    alpha = Image.new("L", (layer_px, layer_px), 0)
    alpha.paste(scaled, (round(layer_px / 2 - center_x), round(layer_px / 2 - center_y)))
    layer = Image.new("RGBA", (layer_px, layer_px), rgb(ink) + (255,))
    layer.putalpha(alpha)
    return layer


# `fit_note` : à 68 dp de large pour toute l'image, le point du dessin le plus éloigné du centre (le bas du manche) est à
# environ 33,7 dp, pour un cercle de sécurité de 33 dp et un masque rond de 36 dp : rien n'est coupé, même sous un masque rond.


def save_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "WEBP", lossless=True, quality=100, method=6)


def play_store_icon(art: Art, background: str, ink: str) -> Image.Image:
    """Plein cadre, fond uni et dessin de la couleur de l'encre."""
    full = Image.new("RGB", art.alpha.size, rgb(background))
    full.paste(rgb(ink), mask=art.alpha)
    return full.resize((512, 512), Image.LANCZOS)


def visible_icon(art: Art, background: str, ink: str, size: int = 432) -> Image.Image:
    """La zone de 72 dp que le lanceur montre, avec le fond et le dessin composés."""
    full = Image.new("RGBA", (size, size), rgb(background) + (255,))
    full.alpha_composite(foreground(art, size, ink))
    inset = round(size * (LAYER_DP - VISIBLE_DP) / 2 / LAYER_DP)
    return full.crop((inset, inset, size - inset, size - inset))


def masked(content: Image.Image, shape: str, backdrop: tuple[int, int, int]) -> Image.Image:
    """Le contenu sous un masque de lanceur (rond, carré arrondi ou carré), posé sur [backdrop]."""
    side = content.width
    mask = Image.new("L", (side * 4, side * 4), 0)
    d = ImageDraw.Draw(mask)
    box = (0, 0, side * 4 - 1, side * 4 - 1)
    if shape == "cercle":
        d.ellipse(box, fill=255)
    elif shape == "carre arrondi":
        d.rounded_rectangle(box, radius=side * 4 * 0.22, fill=255)
    else:  # carré plein : toute la zone de 72 dp, le cas le plus exigeant pour les bords
        d.rectangle(box, fill=255)
    mask = mask.resize((side, side), Image.LANCZOS)
    tile = Image.new("RGBA", (side, side), backdrop + (255,))
    tile.paste(content, (0, 0), mask)
    return tile


def font(size: int) -> ImageFont.ImageFont:
    for name in ("segoeui.ttf", "arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def preview_sheet(art: Art, background: str, ink: str, out: Path) -> None:
    """Planche de contrôle : le calque complet, puis la zone visible sous plusieurs masques de lanceur."""
    size = 432
    full = Image.new("RGBA", (size, size), rgb(background) + (255,))
    full.alpha_composite(foreground(art, size, ink))
    visible = visible_icon(art, background, ink, size)
    side = visible.width

    # Icône thématique d'Android 13+ : le système ne garde que la transparence et colore le dessin sur un fond de sa palette.
    tinted = Image.new("RGBA", (size, size), (74, 68, 88, 255))
    tint = Image.new("RGBA", (size, size), (232, 222, 248, 255))
    tint.putalpha(foreground(art, size, ink).getchannel("A"))
    tinted.alpha_composite(tint)
    inset = round(size * (LAYER_DP - VISIBLE_DP) / 2 / LAYER_DP)
    themed = tinted.crop((inset, inset, size - inset, size - inset))

    backdrop = (245, 245, 245)
    tiles = [
        masked(visible, "cercle", backdrop),
        masked(visible, "carre arrondi", backdrop),
        masked(visible, "carre", backdrop),
        masked(themed, "cercle", backdrop),
    ]
    sheet = Image.new("RGBA", (size + (side + 24) * len(tiles) + 48, size + 48), backdrop + (255,))
    sheet.paste(full, (24, 24))
    for i, tile in enumerate(tiles):
        sheet.paste(tile, (size + 48 + i * (side + 24), 24))
    sheet.convert("RGB").save(out)


def variants_board(art: Art, out: Path) -> None:
    """Compare les variantes de [VARIANTS] : rond et carré arrondi, sur un fond de bureau sombre puis clair."""
    icon = 190
    gap = 28
    label_h = 44
    width = gap + len(VARIANTS) * (icon + gap) + 0
    row_h = icon + label_h + 2 * gap
    board = Image.new("RGB", (width, 2 * row_h + 20), (0, 0, 0))
    draw = ImageDraw.Draw(board)
    label_font = font(17)
    for row, (backdrop, ink_text) in enumerate([((22, 26, 34), (235, 238, 245)), ((236, 236, 241), (30, 32, 40))]):
        top = row * (row_h + 20)
        draw.rectangle((0, top, width, top + row_h), fill=backdrop)
        for i, (name, background, ink) in enumerate(VARIANTS):
            x = gap + i * (icon + gap)
            content = visible_icon(art, background, ink).resize((icon, icon), Image.LANCZOS)
            board.paste(masked(content, "cercle", backdrop).convert("RGB"), (x, top + gap))
            draw.text((x, top + gap + icon + 10), name, fill=ink_text, font=label_font)
    board.save(out)


def argument(name: str, default: str | None = None) -> str | None:
    return sys.argv[sys.argv.index(name) + 1] if name in sys.argv else default


def main() -> None:
    source = Image.open(SRC)
    if source.width != source.height:
        sys.exit(f"{SRC.name} doit être carré ({source.size}).")
    art = Art(source)

    if "--variants" in sys.argv:
        out = Path(argument("--variants"))
        variants_board(art, out)
        print(f"planche des variantes : {out}")
        return

    background = argument("--background", BACKGROUND)
    ink = argument("--ink", INK)
    print(f"fond {background}, encre {ink}, dessin dans {art.bbox}")

    for name, scale in DENSITIES.items():
        layer = foreground(art, round(LAYER_DP * scale), ink)
        save_webp(layer, RES / f"mipmap-{name}" / "ic_launcher_foreground.webp")
        print(f"mipmap-{name}: {layer.width}px")
        obsolete = RES / f"mipmap-{name}" / "ic_launcher_background.webp"
        if obsolete.exists():
            obsolete.unlink()
            print(f"  supprimé : {obsolete.relative_to(PROJECT)} (le fond est maintenant une couleur)")

    colors = RES / "values" / "ic_launcher_background.xml"
    colors.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Généré par tools/make_icons.py : ne pas modifier à la main. -->\n"
        "<resources>\n"
        f'    <color name="ic_launcher_background">{background.upper()}</color>\n'
        "</resources>\n",
        encoding="utf-8",
    )
    print(f"couleur de fond : {colors.relative_to(PROJECT)}")

    play_store_icon(art, background, ink).save(PLAY_STORE, optimize=True)
    print(f"fiche Play Store : {PLAY_STORE.relative_to(PROJECT)}")

    if "--preview" in sys.argv:
        out = Path(argument("--preview"))
        preview_sheet(art, background, ink, out)
        print(f"planche de contrôle : {out}")


if __name__ == "__main__":
    main()
