package com.blackstack.beatbox

import com.blackstack.beatbox.ui.library.libraryLetter
import com.blackstack.beatbox.ui.library.librarySortKey
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Kütüphane A-Z indekslemesinin dil davranışını doğrular.
 */
class LibraryIndexingTest : StringSpec({

    "Turkish letters fold into A-Z sections" {
        libraryLetter("Çilek") shouldBe 'C'
        libraryLetter("İstanbul") shouldBe 'I'
        libraryLetter("ışık") shouldBe 'I'
        libraryLetter("Şeker") shouldBe 'S'
        libraryLetter("Özgür") shouldBe 'O'
        libraryLetter("Üzüm") shouldBe 'U'
        libraryLetter("Ğarip") shouldBe 'G'
    }

    "digits and non-Latin titles share the hash section" {
        libraryLetter("42") shouldBe '#'
        libraryLetter("東京") shouldBe '#'
        libraryLetter("") shouldBe '#'
    }

    "sort keys ignore case and diacritics" {
        librarySortKey("Çilek") shouldBe "cilek"
        librarySortKey("  İstanbul ") shouldBe "istanbul"
        librarySortKey("ŞEKER") shouldBe "seker"
    }
})
