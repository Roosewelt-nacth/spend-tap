import sys
sys.argv = [sys.argv[0], "icon3-out"]
exec(open("make_icon2.py").read().split("variants = {")[0])   # reuse helpers only

mark = ribbon_mark(2.6, 10.0)                       # white mark on transparent, 2048 canvas = 108 units
full = compose(mark, "plain")
full.resize((1024, 1024), Image.LANCZOS).convert("RGB").save(OUT / "icon-1024.png")
full.resize((512, 512), Image.LANCZOS).convert("RGB").save(OUT / "icon-512.png")

# Android adaptive foreground: white mark on transparent, per density (108dp canvas), 10% smaller for mask safety
small = int(SS * 0.9)
shrunk = Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
shrunk.paste(mark.resize((small, small), Image.LANCZOS), ((SS - small) // 2, (SS - small) // 2))
for name, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
    shrunk.resize((px, px), Image.LANCZOS).save(OUT / f"fg-{name}.png")

# Quick Settings tile / shortcut: crop to the mark, white on transparent, 96px
bbox = mark.getbbox()
crop = mark.crop(bbox)
side = max(crop.size)
tile = Image.new("RGBA", (side, side), (0, 0, 0, 0))
tile.paste(crop, ((side - crop.width) // 2, (side - crop.height) // 2))
tile.resize((96, 96), Image.LANCZOS).save(OUT / "ic_tile.png")

# preview on black, rounded + circle
sheet = Image.new("RGB", (1000, 520), (38, 38, 42))
for i, shape in enumerate(["round", "circle"]):
    t, m = mask_preview(full, 440, shape)
    sheet.paste(t, (40 + i * 480, 40), m)
sheet.save(OUT / "preview.png")
print("ok", bbox)
