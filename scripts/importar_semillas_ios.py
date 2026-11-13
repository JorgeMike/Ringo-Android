#!/usr/bin/env python3
"""Convierte las semillas de iOS (Data/SeedData/*.swift) en catálogos de Kotlin.

Uso: python3 scripts/importar_semillas_ios.py ../TiempoFuera-iOS/Ringo-iOS/Data/SeedData

Genera `data/catalogo/Catalogo{Luchadores,Misiones,Ligas}.kt`. Si Jacobo agrega o
corrige luchadores, misiones o ligas en iOS, se vuelve a correr y se revisa el diff.
"""
import re
import sys
from pathlib import Path

DESTINO = Path(__file__).resolve().parent.parent / "app/src/main/java/com/rudoestudio/tiempofuera/data/catalogo"
PAQUETE = "com.rudoestudio.tiempofuera.data.catalogo"

TIPOS_GRUPO = {"distraccion": "DISTRACCION", "lectura": "EDUCATIVAS", "salud": "SALUD", "productividad": "PRODUCTIVIDAD"}
CAMPOS_DOUBLE = ("distanciaObjetivoMetros", "umbralCercaniaCM", "posicionX", "posicionY")


def snake(nombre: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", nombre).upper()


def bloques(texto: str, apertura: str) -> list[str]:
    """Cada `apertura(...)` completo, respetando paréntesis anidados y strings."""
    resultado = []
    i = 0
    while (inicio := texto.find(apertura + "(", i)) != -1:
        j, nivel, en_string = inicio + len(apertura), 0, False
        while True:
            c = texto[j]
            if en_string:
                if c == "\\":
                    j += 1
                elif c == '"':
                    en_string = False
            elif c == '"':
                en_string = True
            elif c == "(":
                nivel += 1
            elif c == ")":
                nivel -= 1
                if nivel == 0:
                    break
            j += 1
        resultado.append(texto[inicio : j + 1])
        i = j + 1
    return resultado


def swift_a_kotlin(codigo: str) -> str:
    """Etiquetas `x: v` → `x = v`, arreglos `[…]` → `listOf(…)`, fuera de los strings."""
    salida, i, en_string = [], 0, False
    while i < len(codigo):
        c = codigo[i]
        if en_string:
            salida.append(c)
            if c == "\\":
                salida.append(codigo[i + 1])
                i += 1
            elif c == '"':
                en_string = False
        elif c == '"':
            en_string = True
            salida.append(c)
        elif c == "[":
            salida.append("listOf(")
        elif c == "]":
            salida.append(")")
        elif c == ":" and codigo[i + 1] == " ":
            salida.append(" =")
        else:
            salida.append(c)
        i += 1
    kotlin = "".join(salida)
    kotlin = re.sub(r"tipo = \.(\w+)", lambda m: f"tipoBase = TipoLuchador.{snake(m.group(1))}", kotlin)
    kotlin = re.sub(r'rareza = "(\w+)"', lambda m: f"rareza = Rareza.{snake(m.group(1))}", kotlin)
    kotlin = re.sub(r"tipoMision = \.(\w+)", lambda m: f"tipoMision = TipoMision.{snake(m.group(1))}", kotlin)
    kotlin = re.sub(r"division = \.(\w+)", lambda m: f"division = DivisionMision.{snake(m.group(1))}", kotlin)
    kotlin = re.sub(r'identificadorGrupoApps = "(\w+)"', lambda m: f"identificadorGrupoApps = TipoGrupoApps.{TIPOS_GRUPO[m.group(1)]}", kotlin)
    for campo in CAMPOS_DOUBLE:
        kotlin = re.sub(rf"({campo} = [\d_]+)(?![\d.])", r"\1.0", kotlin)
    kotlin = kotlin.replace("MisionModel(", "Mision(").replace("LuchadorModel(", "Luchador(")
    return kotlin


def reindentar(bloque: str, sangria: str) -> str:
    lineas = bloque.strip().splitlines()
    base = min((len(l) - len(l.lstrip()) for l in lineas[1:] if l.strip()), default=0)
    return "\n".join(sangria + (l[base:] if i else l.strip()) for i, l in enumerate(lineas))


def luchadores(carpeta: Path) -> str:
    texto = next(carpeta.glob("*LuchadoresSeedData.swift")).read_text()
    entradas = []
    for b in bloques(texto, "LuchadorModel"):
        k = swift_a_kotlin(b)
        k = re.sub(r"cantidad = \d+,?\s*", "", k)
        k = re.sub(r",(\s*)\)$", r"\1)", k.rstrip())
        k = k[:-1].rstrip().rstrip(",") + ",\n            cantidad = 0,\n            puntosPoder = 0,\n        )"
        entradas.append(reindentar(k, "    "))
    return f"""package {PAQUETE}

import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador

// GENERADO por scripts/importar_semillas_ios.py desde iOS (LuchadoresSeedData).
// No editar a mano: corregir en iOS y volver a correr el script.

/** Los luchadores tal como vienen de fábrica: sin recolectar, con su nivel inicial. */
val CATALOGO_LUCHADORES: List<Luchador> = listOf(
{",\n".join(entradas)},
)
"""


def misiones(carpeta: Path) -> str:
    texto = next(carpeta.glob("*MisionesSeedData.swift")).read_text()
    entradas = [reindentar(swift_a_kotlin(b), "    ") for b in bloques(texto, "\n        Mision")]
    return f"""package {PAQUETE}

{IMPORTS_MISION}

// GENERADO por scripts/importar_semillas_ios.py desde iOS (MisionesSeedData).
// No editar a mano: corregir en iOS y volver a correr el script.

/** Misiones diarias (ids 1000+), repartidas por el MotorDeMisiones. */
val CATALOGO_MISIONES_DIARIAS: List<Mision> = listOf(
{",\n".join(entradas)},
)
"""


IMPORTS_MISION = """import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.ElementoEnEscena
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.misiones.Secuencia
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision"""


def ligas(carpeta: Path) -> str:
    texto = next(carpeta.glob("*LigasSeedData.swift")).read_text()
    misiones_liga = [reindentar(swift_a_kotlin(b), "    ") for b in bloques(texto, "MisionModel")]
    # Las de la migración 401 → 404 también son `MisionModel(`, pero sin argumentos: se descartan.
    misiones_liga = [m for m in misiones_liga if "id = " in m]
    entradas = []
    for b in bloques(texto, "LigaModel"):
        if "id:" not in b:
            continue
        campos = dict(re.findall(r"(\w+): (\[[^\]]*\]|[^,\n]+)", b))
        estrella = campos["luchadorEstrellaId"]
        # En iOS: `misionIds: misionesX.map { $0.id }`; las misiones de la liga N tienen ids N01–N99.
        todos_ids = [int(m) for m in re.findall(r"\bid = (\d+)", "\n".join(misiones_liga))]
        ids_misiones = [m for m in todos_ids if m // 100 == int(campos["id"])]
        luchadores_liga = campos["luchadoresLiga"].strip("[]")
        entradas.append(
            f"""    LigaDefinicion(
        id = {campos["id"]},
        nombre = {campos["name"]},
        luchadorEstrellaId = {estrella},
        misionIds = listOf({", ".join(map(str, ids_misiones))}),
        luchadoresLigaIds = listOf({luchadores_liga}),
    )"""
        )
    return f"""package {PAQUETE}

{IMPORTS_MISION}

// GENERADO por scripts/importar_semillas_ios.py desde iOS (LigasSeedData).
// No editar a mano: corregir en iOS y volver a correr el script.

/** Una liga antes de resolver sus luchadores y misiones (en iOS: `LigaModel`). */
data class LigaDefinicion(
    val id: Int,
    val nombre: String,
    val luchadorEstrellaId: Int,
    val misionIds: List<Int>,
    /** Puede repetir ids: un luchador repetido sale más en los sobres de la liga. */
    val luchadoresLigaIds: List<Int>,
)

/** Misiones de liga (ids 101–499): se repiten diario y dan experiencia. */
val CATALOGO_MISIONES_LIGA: List<Mision> = listOf(
{",\n".join(misiones_liga)},
)

val CATALOGO_LIGAS: List<LigaDefinicion> = listOf(
{",\n".join(entradas)},
)
"""


def main() -> None:
    carpeta = Path(sys.argv[1])
    DESTINO.mkdir(parents=True, exist_ok=True)
    (DESTINO / "CatalogoLuchadores.kt").write_text(luchadores(carpeta))
    (DESTINO / "CatalogoMisiones.kt").write_text(misiones(carpeta))
    (DESTINO / "CatalogoLigas.kt").write_text(ligas(carpeta))
    print(f"Catálogos escritos en {DESTINO}")


if __name__ == "__main__":
    main()
