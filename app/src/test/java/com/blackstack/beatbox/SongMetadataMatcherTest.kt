package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Çevrim içi şarkı eşleştirmesinin güvenli çalışmasını doğrular.
 *
 * Örnekler LDPlayer'daki metadata'sız MP3 dosyalarından alınmıştır; bu
 * dosyalarda sanatçı etiketi `<unknown>` ve süreler kopyaya göre değişebilir.
 */
class SongMetadataMatcherTest : StringSpec({

    "Türkçe karakterler ve noktalama normalize edilir" {
        SongMetadataMatcher.normalize("Mavi Gri - Güzel Kadın (Official Video)")
            .shouldBe("mavi gri guzel kadin")
        SongMetadataMatcher.normalize("BLOK3 — KUSURA BAKMA")
            .shouldBe("blok3 kusura bakma")
    }

    "bilinmeyen sanatçı varyantları tanınır" {
        SongMetadataMatcher.isUnknownArtist(null) shouldBe true
        SongMetadataMatcher.isUnknownArtist("") shouldBe true
        SongMetadataMatcher.isUnknownArtist("<unknown>") shouldBe true
        SongMetadataMatcher.isUnknownArtist("Unknown Artist") shouldBe true
        SongMetadataMatcher.isUnknownArtist("Bilinmeyen Sanatçı") shouldBe true
        SongMetadataMatcher.isUnknownArtist("Mavi Gri") shouldBe false
    }

    "sanatçısı bilinmeyen şarkıda başlıktaki sanatçı eşleşmesi kabul edilir" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "Mavi Gri Güzel Kadın",
            queryArtist = null,
            queryDurationMs = 214_000L,
            candidates = listOf(
                candidate("Güzel Kadın", "Mavi Gri", 204),
                candidate("Duy Beni", "Mavi Gri", 252),
                candidate("Jesus For A Day", "Macy Gray", 210)
            )
        )
        match?.artist shouldBe "Mavi Gri"
        match?.title shouldBe "Güzel Kadın"
    }

    "canlı sürüm yerine süreye uyan stüdyo sürümü seçilir" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "Mavi Gri İlaç Ol Yaralarıma",
            queryArtist = null,
            queryDurationMs = 160_000L,
            candidates = listOf(
                candidate("İlaç Ol Yaralarıma (Live Session)", "Mavi Gri", 143),
                candidate("İlaç Ol Yaralarıma", "Mavi Gri", 160),
                candidate("İlaç Ol Yaralarıma", "Médis", 155)
            )
        )
        match?.title shouldBe "İlaç Ol Yaralarıma"
        match?.durationSeconds shouldBe 160
    }

    "boşluklu ve bitişik sanatçı adları eşleşir" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "Blok 3 Kusura Bakma",
            queryArtist = null,
            queryDurationMs = 173_000L,
            candidates = listOf(
                candidate("KUSURA BAKMA", "BLOK3", 157),
                candidate("KUSURA BAKMA (blok-3 cover rap)", "bekir önem", 195)
            )
        )
        match?.artist shouldBe "BLOK3"
    }

    "tireli dosya adlarında sanatçı ve şarkı ayrılabilir" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "BLOK3 - Sebebi yar",
            queryArtist = null,
            queryDurationMs = 198_000L,
            candidates = listOf(
                candidate("Sebebi Yar", "BLOK3", 186),
                candidate("Sebebi Yar I Blok Medis", "Médis", 155)
            )
        )
        match?.artist shouldBe "BLOK3"
    }

    "kısa sanatçı adı başlık içinde yakalanır" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "YSN - Yan Sen Derdine",
            queryArtist = null,
            queryDurationMs = 138_000L,
            candidates = listOf(
                candidate("Yan Sen Derdine", "Ysn", 138),
                candidate("Umrumdışı", "Mabel Matiz", 218)
            )
        )
        match?.artist shouldBe "Ysn"
    }

    "bilinen sanatçıyla uyuşmayan sonuç reddedilir" {
        SongMetadataMatcher.pickBest(
            queryTitle = "Duy Beni",
            queryArtist = "Mavi Gri",
            queryDurationMs = 253_000L,
            candidates = listOf(
                candidate("Duy Beni", "Macy Gray", 252)
            )
        ).shouldBeNull()
    }

    "sanatçısı bilinmeyen şarkıda süre çok farklıysa sonuç reddedilir" {
        SongMetadataMatcher.pickBest(
            queryTitle = "Güzel Kadın",
            queryArtist = null,
            queryDurationMs = 214_000L,
            candidates = listOf(
                candidate("Güzel Kadın", "Başka Sanatçı", 300)
            )
        ).shouldBeNull()
    }

    "başlık ve sanatçı birebir eşleşirse farklı sürüm kapağı kabul edilir" {
        val match = SongMetadataMatcher.pickBest(
            queryTitle = "Güzel Kadın",
            queryArtist = "Mavi Gri",
            queryDurationMs = 214_000L,
            candidates = listOf(
                candidate("Güzel Kadın", "Mavi Gri", 300)
            )
        )
        match?.artist shouldBe "Mavi Gri"
    }

    "aday yoksa mevcut bilgiler korunur" {
        SongMetadataMatcher.pickBest(
            queryTitle = "Bilinmeyen Şarkı",
            queryArtist = null,
            queryDurationMs = 180_000L,
            candidates = emptyList()
        ).shouldBeNull()
    }

    "kısmi başlık ancak güçlü sanatçı ve süreyle kabul edilir" {
        SongMetadataMatcher.pickBest(
            queryTitle = "Aşk Ve Sevgi",
            queryArtist = "Mavi Gri",
            queryDurationMs = 180_000L,
            candidates = listOf(
                candidate("Aşk", "Mavi Gri", 180)
            )
        ).shouldBeNull()
    }
})

private fun candidate(
    title: String,
    artist: String,
    durationSeconds: Int,
    artworkUrl: String? = "https://example.com/cover.jpg"
) = SongMetadataCandidate(
    title = title,
    artist = artist,
    durationSeconds = durationSeconds,
    artworkUrl = artworkUrl,
    source = "test"
)
