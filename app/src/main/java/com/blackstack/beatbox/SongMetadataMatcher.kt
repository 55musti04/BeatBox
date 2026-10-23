package com.blackstack.beatbox

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Çevrim içi arama sonucundan yerel şarkıya en uygun eşleşmeyi seçen saf
 * Kotlin yardımcıları. Ağ ve Android bağımlılığı yoktur; JVM testlerinde
 * doğrulanır.
 *
 * Amaç, eksik sanatçı adını ve albüm kapağını otomatik tamamlamak; ancak
 * emin olunamayan durumlarda mevcut bilgileri kesinlikle değiştirmemek.
 */
data class SongMetadataCandidate(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val artworkUrl: String? = null,
    val source: String = ""
)

object SongMetadataMatcher {

    private val unknownArtists = setOf(
        "", "unknown", "unknown artist", "<unknown>", "<unknown artist>",
        "bilinmeyen sanatçı", "<bilinmeyen sanatçı>", "artiste inconnu",
        "unbekannter künstler", "artista desconocido", "artista sconosciuto",
        "неизвестный исполнитель", "不明なアーティスト", "未知艺术家",
        "فنان غير معروف", "알 수 없는 아티스트"
    )

    /** MediaStore'un "bilinmiyor" varyantlarını yakalar. */
    fun isUnknownArtist(artist: String?): Boolean {
        if (artist == null) return true
        return unknownArtists.contains(artist.trim().lowercase(Locale.ROOT))
    }

    /**
     * Karşılaştırma için metni sadeleştirir: küçük harf, Türkçe karakterlerin
     * ASCII karşılığı, parantez içi eklerin atılması ve noktalama temizliği.
     */
    fun normalize(text: String): String {
        var value = text.lowercase(Locale.ROOT)
        value = value
            .replace('ı', 'i')
            .replace('ş', 's')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace('â', 'a')
            .replace('î', 'i')
            .replace('û', 'u')
        value = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        value = value
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("\\b(feat|ft|featuring)\\b.*$"), " ")
            .replace(Regex("^\\d{1,3}[\\s._-]+"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        return value
    }

    private val stopwords = setOf("ve", "the", "and", "with", "ile", "feat", "ft", "featuring")
    private val versionWords = setOf(
        "live", "remix", "acoustic", "version", "session", "edit",
        "instrumental", "karaoke", "demo", "radio", "extended"
    )

    private fun tokens(normalized: String): Set<String> =
        normalized.split(' ').filter { it.isNotBlank() }.toSet()

    private fun compact(normalized: String): String = normalized.replace(" ", "")

    private fun jaccard(first: Set<String>, second: Set<String>): Double {
        if (first.isEmpty() || second.isEmpty()) return 0.0
        val intersection = first.intersect(second).size
        val union = first.union(second).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    /**
     * Başlık benzerliği. Dosya adında sanatçı da varsa ("Mavi Gri - Duy Beni")
     * bu fazlalık açıklanmış sayılır; tek kelimelik bir aday başlık, sorgudaki
     * ilgisiz kelimeleri yutarak yanlış eşleşme üretemez.
     */
    private fun titleSimilarity(
        queryTitle: String,
        candidateTitle: String,
        candidateArtist: String
    ): Double {
        val query = normalize(queryTitle)
        val candidate = normalize(candidateTitle)
        if (query.isBlank() || candidate.isBlank()) return 0.0
        if (query == candidate) return 1.0

        val queryTokens = tokens(query)
        val candidateTokens = tokens(candidate)
        if (queryTokens.isEmpty() || candidateTokens.isEmpty()) return 0.0

        val artistTokens = tokens(normalize(candidateArtist))
        val artistCompact = compact(normalize(candidateArtist))

        // Aday başlık sorgunun içinde: "Mavi Gri Güzel Kadın" / "Güzel Kadın".
        if (candidateTokens.all { it in queryTokens }) {
            val remainderTokens = queryTokens - candidateTokens - artistTokens - stopwords
            val remainderCompact = compact(query).replaceFirst(compact(candidate), "")
            val afterArtist = if (artistCompact.isNotBlank()) {
                remainderCompact.replaceFirst(artistCompact, "")
            } else {
                remainderCompact
            }
            val explained = remainderTokens.isEmpty() || afterArtist.isBlank()
            return if (candidateTokens.size >= 2 || explained) 1.0 else jaccard(queryTokens, candidateTokens)
        }

        // Sorgu başlık adayın içinde: genellikle canlı/remix sürüm farkı.
        if (queryTokens.all { it in candidateTokens }) {
            if (queryTokens.size >= 2) return 1.0
            val extra = tokens(compact(candidate).replaceFirst(compact(query), ""))
            return if (extra.all { it in versionWords }) 1.0 else jaccard(queryTokens, candidateTokens)
        }

        return jaccard(queryTokens, candidateTokens)
    }

    private fun artistSimilarity(
        queryArtist: String?,
        queryTitle: String,
        candidateArtist: String
    ): Double {
        val candidate = normalize(candidateArtist)
        if (candidate.isBlank()) return 0.35

        if (!isUnknownArtist(queryArtist)) {
            val query = normalize(queryArtist.orEmpty())
            if (query == candidate) return 1.0
            val queryTokens = tokens(query)
            val candidateTokens = tokens(candidate)
            if (queryTokens.isEmpty() || candidateTokens.isEmpty()) return 0.0
            if (candidateTokens.all { it in queryTokens } || queryTokens.all { it in candidateTokens }) {
                return 1.0
            }
            val intersection = queryTokens.intersect(candidateTokens).size
            val union = queryTokens.union(candidateTokens).size
            return if (union == 0) 0.0 else intersection.toDouble() / union
        }

        // Sanatçı etiketi yok: aday sanatçı şarkı başlığının içinde geçiyor mu?
        val title = normalize(queryTitle)
        val titleTokens = tokens(title)
        val candidateTokens = tokens(candidate)
        if (candidateTokens.isNotEmpty() && candidateTokens.all { it in titleTokens }) {
            return 1.0
        }
        val compactCandidate = compact(candidate)
        if (compactCandidate.isNotBlank() && compact(title).contains(compactCandidate)) {
            return 1.0
        }
        // Başlıkta sanatçı yok; süre kontrolüyle birlikte temkinli davran.
        return 0.35
    }

    private fun durationSimilarity(queryDurationMs: Long, candidateSeconds: Int): Double {
        if (queryDurationMs <= 0 || candidateSeconds <= 0) return 0.5
        val difference = abs(queryDurationMs / 1000.0 - candidateSeconds)
        return when {
            difference <= 3.0 -> 1.0
            difference >= 45.0 -> 0.0
            else -> 1.0 - (difference - 3.0) / 42.0
        }
    }

    fun score(
        queryTitle: String,
        queryArtist: String?,
        queryDurationMs: Long,
        candidate: SongMetadataCandidate
    ): Double {
        val title = titleSimilarity(queryTitle, candidate.title, candidate.artist)
        val artist = artistSimilarity(queryArtist, queryTitle, candidate.artist)
        val duration = durationSimilarity(queryDurationMs, candidate.durationSeconds)
        return 0.50 * title + 0.35 * artist + 0.15 * duration
    }

    /**
     * Adaylar arasından güven eşiğini geçen en iyi eşleşmeyi döndürür.
     * Eşik geçilmezse null döner ve çağıran taraf mevcut bilgileri korur.
     */
    fun pickBest(
        queryTitle: String,
        queryArtist: String?,
        queryDurationMs: Long,
        candidates: List<SongMetadataCandidate>
    ): SongMetadataCandidate? {
        if (candidates.isEmpty()) return null

        val best = candidates.maxByOrNull {
            score(queryTitle, queryArtist, queryDurationMs, it)
        } ?: return null

        val title = titleSimilarity(queryTitle, best.title, best.artist)
        val artist = artistSimilarity(queryArtist, queryTitle, best.artist)
        val duration = durationSimilarity(queryDurationMs, best.durationSeconds)

        val accepted = when {
            // Başlık ve sanatçı birebir: süre farkı farklı sürümlerde olabilir.
            title >= 0.95 && artist >= 0.80 -> true
            // Başlık net, sanatçı başlıktan çıkarılamadı: süre yakın olmalı.
            title >= 0.95 && artist >= 0.30 && duration >= 0.65 -> true
            // Kısmi başlık eşleşmesi ancak sanatçı ve süre güçlüyse kabul.
            title >= 0.75 && artist >= 0.80 && duration >= 0.55 -> true
            else -> false
        }
        return if (accepted) best else null
    }
}
