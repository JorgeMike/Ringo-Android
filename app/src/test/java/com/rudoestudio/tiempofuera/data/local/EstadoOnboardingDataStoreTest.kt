package com.rudoestudio.tiempofuera.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rudoestudio.tiempofuera.data.onboarding.EstadoOnboardingDataStore
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoOnboarding
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileBorrador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class EstadoOnboardingDataStoreTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repo by lazy {
        EstadoOnboardingDataStore(PreferenceDataStoreFactory.create(scope = alcance) { carpeta.newFile("onboarding.preferences_pb") })
    }

    private val progreso = ProgresoOnboarding(
        PasoOnboarding.CONF_PERMISOS,
        UserProfileBorrador(
            fechaNacimiento = LocalDate.of(2010, 5, 3),
            email = "luchador@rudo.mx",
            proposito = PropositoUsuario.CUIDAR_CUERPO,
            personalidadId = TipoPersonalidad.EXPLORADOR,
            divisionesSeleccionadas = setOf(DivisionMision.EJERCICIO, DivisionMision.LECTURA, DivisionMision.CONECTAR),
            tiempoInicial = 300,
        ),
    )

    @After
    fun cerrar() = alcance.cancel()

    @Test
    fun `guarda el paso y las respuestas para retomar`() = runTest {
        assertNull(repo.obtenerProgreso())
        repo.guardarProgreso(progreso)
        assertEquals(progreso, repo.obtenerProgreso())
    }

    @Test
    fun `al finalizar borra el progreso y reiniciar vuelve a mostrarlo`() = runTest {
        repo.guardarProgreso(progreso)
        repo.finalizar()
        assertTrue(repo.finalizado.first())
        assertNull(repo.obtenerProgreso())
        repo.reiniciar()
        assertFalse(repo.finalizado.first())
    }
}
