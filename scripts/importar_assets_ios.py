#!/usr/bin/env python3
"""
Importa los assets de la app iOS (Assets.xcassets) a recursos de Android (M0.7).

    python3 scripts/importar_assets_ios.py [ruta/a/Assets.xcassets]

Por omisión lee ../TiempoFuera-iOS/Ringo-iOS/Assets.xcassets. Se puede volver a correr
cuando cambien los assets en iOS: reescribe los archivos generados y la tabla
docs/ASSETS.md.

- PNG  -> res/drawable-nodpi/  (iOS los tiene en una sola escala "1x": no se reescalan)
- SVG  -> res/drawable-xxxhdpi/ rasterizados a 4x con Inkscape. 9 SVG traen imágenes
          incrustadas y otros usan máscaras, que VectorDrawable no soporta; así todos
          quedan igual que en iOS. 1 pt de iOS = 1 dp de Android.
- WAV  -> res/raw/
- Nombre del recurso = nombre del imageset/dataset en snake_case (Android solo acepta
  [a-z0-9_]). Ej.: "FondoTitulos2" -> fondo_titulos2, "sticker-1" -> sticker_1.
"""
import json
import re
import shutil
import subprocess
import sys
import unicodedata
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
RES = RAIZ / "app/src/main/res"
XCASSETS_DEFAULT = RAIZ.parent / "TiempoFuera-iOS/Ringo-iOS/Assets.xcassets"
ESCALA_SVG = 4  # xxxhdpi
# Lo que generó la última corrida, para borrarlo antes de la siguiente.
LISTA_GENERADOS = RAIZ / "scripts/assets_importados.txt"


def sin_acentos(texto: str) -> str:
    return unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode()


def snake(nombre: str) -> str:
    s = re.sub(r"[^0-9A-Za-z]+", "_", sin_acentos(nombre))  # "Doña" -> "dona"
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", s)
    s = re.sub(r"_+", "_", s).strip("_").lower()
    if not s or s[0].isdigit():
        s = "img_" + s
    return s


def archivo_de(set_dir: Path, extension: str) -> Path | None:
    contenido = json.loads((set_dir / "Contents.json").read_text())
    # Los archivos hechos en Mac guardan acentos en NFD y el JSON en NFC: comparar normalizado.
    en_disco = {unicodedata.normalize("NFC", p.name): p for p in set_dir.iterdir()}
    # imageset -> "images"; dataset (sonidos) -> "data"
    for item in contenido.get("images", []) + contenido.get("data", []):
        nombre = item.get("filename")
        if nombre and nombre.lower().endswith(extension):
            return en_disco.get(unicodedata.normalize("NFC", nombre))
    return None


def main() -> int:
    xcassets = Path(sys.argv[1]) if len(sys.argv) > 1 else XCASSETS_DEFAULT
    if not xcassets.is_dir():
        print(f"No encuentro {xcassets}", file=sys.stderr)
        return 1

    destinos = {
        "png": RES / "drawable-nodpi",
        "svg": RES / "drawable-xxxhdpi",
        "wav": RES / "raw",
    }
    # Borrar lo importado antes para que un asset eliminado en iOS no se quede aquí.
    if LISTA_GENERADOS.exists():
        for linea in LISTA_GENERADOS.read_text().splitlines():
            (RES / linea).unlink(missing_ok=True)
    for d in destinos.values():
        d.mkdir(parents=True, exist_ok=True)

    filas, generados = [], []
    sets = sorted(
        p for p in xcassets.rglob("*")
        if p.is_dir() and p.suffix in (".imageset", ".dataset")
    )
    # iOS distingue "LaCalaverita" de "La Calaverita"; en snake_case chocan. A los que
    # chocan se les antepone su carpeta: ilustraciones_la_calaverita, luchadores_la_calaverita.
    conteo: dict[str, int] = {}
    for s in sets:
        conteo[snake(s.stem)] = conteo.get(snake(s.stem), 0) + 1
    vistos: dict[str, Path] = {}
    for s in sets:
        origen = archivo_de(s, ".svg") or archivo_de(s, ".png") or archivo_de(s, ".wav")
        if origen is None:
            continue
        tipo = origen.suffix.lower().lstrip(".")
        nombre = snake(s.stem)
        if conteo[nombre] > 1:
            nombre = f"{snake(s.parent.name)}_{nombre}"
            print(f"Nombre repetido en iOS, se renombra: {s.relative_to(xcassets)} -> {nombre}")
        if nombre in vistos:
            print(f"Nombre repetido {nombre}: {vistos[nombre]} y {s}", file=sys.stderr)
            return 1
        vistos[nombre] = s
        carpeta = destinos[tipo]
        if tipo == "png":
            destino = carpeta / f"{nombre}.png"
            shutil.copyfile(origen, destino)
        elif tipo == "svg":
            destino = carpeta / f"{nombre}.png"
            subprocess.run(
                ["inkscape", str(origen), "--export-type=png",
                 f"--export-dpi={96 * ESCALA_SVG}", f"--export-filename={destino}"],
                check=True, capture_output=True,
            )
        else:
            destino = carpeta / f"{nombre}.wav"
            shutil.copyfile(origen, destino)
        generados.append(str(destino.relative_to(RES)))
        ref = f"R.raw.{nombre}" if tipo == "wav" else f"R.drawable.{nombre}"
        filas.append((str(s.relative_to(xcassets).parent), s.stem, tipo, ref))

    LISTA_GENERADOS.write_text("\n".join(generados) + "\n")
    docs = RAIZ / "docs"
    docs.mkdir(exist_ok=True)
    tabla = [
        "# Assets importados de iOS",
        "",
        "Generado por `scripts/importar_assets_ios.py`: no editar a mano. Para buscar el",
        "equivalente de `Image(\"Nombre\")` de iOS.",
        "",
        "| Carpeta en iOS | Nombre en iOS | Tipo | Android |",
        "|---|---|---|---|",
    ]
    tabla += [f"| {c or '—'} | `{n}` | {t} | `{r}` |" for c, n, t, r in filas]
    (docs / "ASSETS.md").write_text("\n".join(tabla) + "\n")
    print(f"{len(filas)} assets importados")
    return 0


if __name__ == "__main__":
    sys.exit(main())
