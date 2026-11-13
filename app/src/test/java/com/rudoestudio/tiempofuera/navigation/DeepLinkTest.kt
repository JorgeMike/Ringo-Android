package com.rudoestudio.tiempofuera.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {

    @Test
    fun `enlace de misión valido`() {
        assertEquals(DeepLink.IdMision(42), DeepLink.desde("ringo://id-mision/42"))
    }

    @Test
    fun `acepta diagonal final`() {
        assertEquals(DeepLink.IdMision(7), DeepLink.desde("ringo://id-mision/7/"))
    }

    @Test
    fun `otro esquema o host no es de la app`() {
        assertNull(DeepLink.desde("https://id-mision/42"))
        assertNull(DeepLink.desde("ringo://otra-cosa/42"))
    }

    @Test
    fun `id que no es numero o falta`() {
        assertNull(DeepLink.desde("ringo://id-mision/abc"))
        assertNull(DeepLink.desde("ringo://id-mision/"))
        assertNull(DeepLink.desde("ringo://id-mision"))
    }

    @Test
    fun `nulo o basura`() {
        assertNull(DeepLink.desde(null))
        assertNull(DeepLink.desde("no es un enlace %%"))
    }
}
