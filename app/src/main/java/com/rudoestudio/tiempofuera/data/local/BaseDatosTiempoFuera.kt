package com.rudoestudio.tiempofuera.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Lo que en iOS vive en SwiftData. El catálogo (luchadores, misiones, ligas) no se
 * guarda aquí: vive en código (`data/catalogo`) y la base solo guarda el progreso del
 * usuario. Así una corrección del catálogo llega sola, sin las migraciones a mano que
 * iOS hace en sus `SeedData`.
 */
@Database(
    entities = [
        LuchadorProgresoEntity::class,
        ProgresoLigaEntity::class,
        ProgresoDiarioEntity::class,
        HistoricoRachaEntity::class,
        PerfilEntity::class,
    ],
    version = 1,
)
@TypeConverters(Convertidores::class)
abstract class BaseDatosTiempoFuera : RoomDatabase() {
    abstract fun luchadores(): LuchadorDao

    abstract fun progresoLigas(): ProgresoLigaDao

    abstract fun progresoDiario(): ProgresoDiarioDao

    abstract fun historicoRacha(): HistoricoRachaDao

    abstract fun perfil(): PerfilDao

    companion object {
        const val NOMBRE = "tiempo_fuera.db"
    }
}

// region Entidades

/**
 * Lo que cambia de un luchador del catálogo. Solo hay fila para los que el usuario ya
 * tocó; los demás salen tal cual del catálogo. En iOS: `LuchadorModel`.
 */
@Entity(tableName = "luchador_progreso")
data class LuchadorProgresoEntity(
    @PrimaryKey val id: Int,
    val cantidad: Int,
    val nivel: Int,
    val puntosPoder: Int,
    /** Clave de `Rareza` ("comun"…). */
    val rareza: String,
    val skinEquipada: String,
)

/** En iOS: `ProgresoLigaModel`. */
@Entity(tableName = "progreso_liga")
data class ProgresoLigaEntity(
    @PrimaryKey val ligaId: Int,
    val totalExp: Double,
    val nivelActualId: Int,
    val nivelesDesbloqueados: Set<Int>,
)

/** En iOS: `ProgresoDiarioModel` (su id es el día como "yyyy-MM-dd"). */
@Entity(tableName = "progreso_diario")
data class ProgresoDiarioEntity(
    @PrimaryKey val dia: LocalDate,
    val misiones: List<Int>,
)

/** En iOS: `HistoricoRachaModel`. */
@Entity(tableName = "historico_racha")
data class HistoricoRachaEntity(
    @PrimaryKey val fecha: LocalDate,
    val usoProtectorRacha: Boolean,
)

/** Una sola fila (id 0). En iOS: `UserProfileModel`. */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = UNICO,
    val email: String = "",
    /** Clave de `TipoPersonalidad`; vacía hasta el onboarding. */
    val personalidadId: String = "",
    val puntosPoder: Int = 0,
    val monedas: Int = 0,
    val diasRacha: Int = 0,
    val protectoresRacha: Int = 0,
    val lastActivityDate: Instant? = null,
    val luchadorSeleccionadoId: Int? = null,
    /** `List<ProgresoUsuarioLiga>` en JSON (ver `PerfilGuardado.kt`). */
    val progresoLigasJson: String = "[]",
    /** Clave de `PropositoUsuario`; vacía hasta el onboarding. */
    val proposito: String = "",
    /** Claves de `DivisionMision`. */
    val divisionesSeleccionadas: List<String> = emptyList(),
    val tiempoInicial: Int = 0,
    val tiempoMaxUso: Int = 0,
    val horaInicioDia: Int = 0,
    val horaFinDia: Int = 0,
    val colchonInicioFin: Int = 0,
) {
    companion object {
        const val UNICO = 0
    }
}

// endregion

// region DAOs

@Dao
interface LuchadorDao {
    @Query("SELECT * FROM luchador_progreso")
    suspend fun todos(): List<LuchadorProgresoEntity>

    @Query("SELECT * FROM luchador_progreso")
    fun observarTodos(): Flow<List<LuchadorProgresoEntity>>

    @Query("SELECT * FROM luchador_progreso WHERE id = :id")
    suspend fun porId(id: Int): LuchadorProgresoEntity?

    @Upsert
    suspend fun guardar(luchador: LuchadorProgresoEntity)

    @Upsert
    suspend fun guardarTodos(luchadores: List<LuchadorProgresoEntity>)
}

@Dao
interface ProgresoLigaDao {
    @Query("SELECT * FROM progreso_liga ORDER BY ligaId")
    suspend fun todos(): List<ProgresoLigaEntity>

    @Query("SELECT * FROM progreso_liga WHERE ligaId = :ligaId")
    suspend fun porLiga(ligaId: Int): ProgresoLigaEntity?

    @Upsert
    suspend fun guardar(progreso: ProgresoLigaEntity)

    @Upsert
    suspend fun guardarTodos(progresos: List<ProgresoLigaEntity>)

    @Query("DELETE FROM progreso_liga WHERE ligaId NOT IN (:ligaIds)")
    suspend fun borrarExcepto(ligaIds: List<Int>)
}

@Dao
interface ProgresoDiarioDao {
    @Query("SELECT * FROM progreso_diario WHERE dia = :dia")
    suspend fun porDia(dia: LocalDate): ProgresoDiarioEntity?

    /** El día se guarda como "yyyy-MM-dd": el orden de texto es el cronológico. */
    @Query("SELECT * FROM progreso_diario ORDER BY dia")
    suspend fun todos(): List<ProgresoDiarioEntity>

    @Upsert
    suspend fun guardar(progreso: ProgresoDiarioEntity)
}

@Dao
interface HistoricoRachaDao {
    @Query("SELECT * FROM historico_racha WHERE fecha = :fecha")
    suspend fun porFecha(fecha: LocalDate): HistoricoRachaEntity?

    @Query("SELECT * FROM historico_racha ORDER BY fecha")
    suspend fun todos(): List<HistoricoRachaEntity>

    @Upsert
    suspend fun guardar(registro: HistoricoRachaEntity)
}

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil WHERE id = ${PerfilEntity.UNICO}")
    suspend fun obtener(): PerfilEntity?

    @Query("SELECT * FROM perfil WHERE id = ${PerfilEntity.UNICO}")
    fun observar(): Flow<PerfilEntity?>

    @Upsert
    suspend fun guardar(perfil: PerfilEntity)
}

// endregion

/** Las listas se guardan como texto separado por comas; las fechas en ISO. */
class Convertidores {
    @TypeConverter
    fun deListaEnteros(lista: List<Int>): String = lista.joinToString(",")

    @TypeConverter
    fun aListaEnteros(texto: String): List<Int> = if (texto.isEmpty()) emptyList() else texto.split(",").map(String::toInt)

    @TypeConverter
    fun deConjuntoEnteros(conjunto: Set<Int>): String = conjunto.sorted().joinToString(",")

    @TypeConverter
    fun aConjuntoEnteros(texto: String): Set<Int> = aListaEnteros(texto).toSet()

    @TypeConverter
    fun deListaTextos(lista: List<String>): String = lista.joinToString(",")

    @TypeConverter
    fun aListaTextos(texto: String): List<String> = if (texto.isEmpty()) emptyList() else texto.split(",")

    @TypeConverter
    fun deFecha(fecha: LocalDate): String = fecha.toString()

    @TypeConverter
    fun aFecha(texto: String): LocalDate = LocalDate.parse(texto)

    @TypeConverter
    fun deInstante(instante: Instant?): Long? = instante?.toEpochMilli()

    @TypeConverter
    fun aInstante(milis: Long?): Instant? = milis?.let(Instant::ofEpochMilli)
}
