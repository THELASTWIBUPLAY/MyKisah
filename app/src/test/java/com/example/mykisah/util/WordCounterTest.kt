package com.example.mykisah.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WordCounterTest {

    @Test
    fun countsPlainWords() {
        assertEquals(4, WordCounter.count("Budi pergi ke kota"))
    }

    @Test
    fun ignoresWikiMarkup() {
        // @[Kota] seharusnya tetap dihitung sebagai 1 kata ("Kota")
        assertEquals(4, WordCounter.count("Budi pergi ke @[Kota]"))
    }

    @Test
    fun emptyStringIsZero() {
        assertEquals(0, WordCounter.count(""))
    }

    @Test
    fun whitespaceOnlyIsZero() {
        assertEquals(0, WordCounter.count("   \n\t  "))
    }
}