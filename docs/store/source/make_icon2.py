"""Black and white infinity logo. Renders variants for comparison.
Usage: python make_icon2.py <out_dir>"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(sys.argv[1]); OUT.mkdir(parents=True, exist_ok=True)
SS = 2048
U = SS / 108.0
WHITE = (255, 255, 255, 255)


def curve(a=29.0, yscale=1.55, s=1.0, n=720):
    """Lemniscate-style infinity centred at (54,54), in vector units. Returns list of (x, y, t)."""
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        den = 1 + s * math.sin(t) ** 2
        x = a * math.cos(t) / den
        y = a * yscale * math.sin(t) * math.cos(t) / den
        pts.append((54 + x, 54 + y, t))
    return pts


def bg(kind):
    img = Image.new("RGBA", (SS, SS), (0, 0, 0, 255))
    if kind == "glow":                      # faint charcoal bloom for depth, still greyscale
        glow = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
        g = ImageDraw.Draw(glow)
        r = SS * 0.55
        g.ellipse([SS / 2 - r, SS / 2 - r, SS / 2 + r, SS / 2 + r], fill=(60, 60, 64, 255))
        glow = glow.filter(ImageFilter.GaussianBlur(SS * 0.20))
        img = Image.alpha_composite(img, glow)
    return img


def dot(d, x, y, r, fill):
    d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def stroke_mark(width_units):
    img = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = width_units * U / 2
    for x, y, _ in curve(n=1400):
        dot(d, x * U, y * U, r, WHITE)
    return img


def ribbon_mark(thin, thick):
    """Calligraphic: thick at the outer ends of each loop, thin where the strokes cross."""
    img = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x, y, t in curve(n=2400):
        k = abs(math.cos(t))                 # 1 at loop tips, 0 at the crossing
        w = thin + (thick - thin) * (k ** 0.8)
        dot(d, x * U, y * U, w * U / 2, WHITE)
    return img


def compose(mark, kind="plain"):
    return Image.alpha_composite(bg(kind), mark)


def mask_preview(full, size, shape):
    tile = full.resize((size, size), Image.LANCZOS).convert("RGB")
    m = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(m)
    if shape == "round":
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=255)
    else:
        md.ellipse([0, 0, size - 1, size - 1], fill=255)
    return tile, m


variants = {
    "A_stroke_plain": compose(stroke_mark(7.0), "plain"),
    "B_ribbon_plain": compose(ribbon_mark(3.2, 9.5), "plain"),
    "C_ribbon_glow": compose(ribbon_mark(3.2, 9.5), "glow"),
}
for name, img in variants.items():
    img.resize((1024, 1024), Image.LANCZOS).convert("RGB").save(OUT / f"{name}-1024.png")

sheet = Image.new("RGB", (1560, 640), (38, 38, 42))
x = 40
for name, img in variants.items():
    tile, m = mask_preview(img, 440, "round")
    sheet.paste(tile, (x, 40), m)
    small, sm = mask_preview(img, 96, "round")
    sheet.paste(small, (x + 40, 520), sm)
    small2, sm2 = mask_preview(img, 48, "round")
    sheet.paste(small2, (x + 170, 544), sm2)
    x += 500
sheet.save(OUT / "compare.png")
print("ok")
