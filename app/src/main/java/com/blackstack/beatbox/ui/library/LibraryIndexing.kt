package com.blackstack.beatbox.ui.library

import com.blackstack.beatbox.AudioFile
import java.text.Normalizer
import java.util.Locale

/**
 * Kütüphane listesini alfabetik bölümlere ayıran saf yardımcılar.
 *
 * Türkçe ve diğer Latin dillerindeki aksanlı harfler ASCII karşılıklarına
 * indirgenir; böylece "Çilek" C bölümünde, "İstanbul" I bölümünde görünür.
 */

private val diacriticMarks = Regex("\\p{Mn}+")

/** Sıralama için küçük harfli, aksansız anahtar üretir. */
internal fun librarySortKey(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(diacriticMarks, "")
        .lowercase(Locale.ROOT)

/**
 * Başlığın ait olduğu A-Z bölümünü döndürür.
 * Latin alfabesine indirgenemeyen başlıklar '#' bölümünde toplanır.
 */
internal fun libraryLetter(title: String): Char {
    val first = title.trim().firstOrNull() ?: return '#'
    if (first.isDigit()) return '#'
    val normalized = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .replace(diacriticMarks, "")
        .uppercase(Locale.ROOT)
    return normalized.firstOrNull()?.takeIf { it in 'A'..'Z' } ?: '#'
}

/** Başlıkları aksan duyarsız biçimde A-Z sıralar. */
internal fun sortedByTitle(songs: List<AudioFile>): List<AudioFile> =
    songs.sortedWith(
        compareBy<AudioFile> { librarySortKey(it.title) }
            .thenBy { it.title }
    )
