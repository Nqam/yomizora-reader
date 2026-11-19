package com.yomizora.reader

import com.yomizora.reader.data.content.GrammarCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GrammarCatalogTest {

    @Test
    fun `itadakemasenka segment matches polite request grammar not invitation or desu_masu`() {
        val matched = GrammarCatalog.findGrammarForSegment("っていただけませんか。」")
        assertEquals("g_n4_te_itadakemasenka", matched?.id)
        assertNotEquals("g_n5_desu_masu", matched?.id)
        assertNotEquals("g_n5_mashou_masenka", matched?.id)
    }

    @Test
    fun `masenka invitation still matches mashou_masenka`() {
        val matched = GrammarCatalog.findGrammarForSegment("に行きませんか。」")
        assertEquals("g_n5_mashou_masenka", matched?.id)
    }
}
