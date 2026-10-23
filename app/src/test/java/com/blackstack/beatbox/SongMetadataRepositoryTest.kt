package com.blackstack.beatbox

import android.net.Uri
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * Otomatik meta veri uygulamasının "mevcut değeri koru" kuralını doğrular.
 */
class SongMetadataRepositoryTest : StringSpec({

    "çözülen sanatçı yalnızca bilinmeyen alana yazılır" {
        val unknown = song(1, "<unknown>")
        val known = song(2, "Mevcut Sanatçı")

        val result = SongMetadataRepository.applyResolved(
            songs = listOf(unknown, known),
            id = 1,
            artist = "Mavi Gri",
            artworkPath = "/art/1.jpg"
        )

        result[0].artist shouldBe "Mavi Gri"
        result[0].artworkPath shouldBe "/art/1.jpg"
        result[1].artist shouldBe "Mevcut Sanatçı"
        result[1].artworkPath shouldBe null
    }

    "yerel kapak çevrim içi kapakla ezilmez" {
        val withLocalArt = song(1, "<unknown>", artworkPath = "/local/cover.jpg")

        val result = SongMetadataRepository.applyResolved(
            songs = listOf(withLocalArt),
            id = 1,
            artist = "Mavi Gri",
            artworkPath = "/online/cover.jpg"
        )

        result.single().artworkPath shouldBe "/local/cover.jpg"
    }

    "eşleşmeyen şarkı değişmeden kalır" {
        val song = song(1, "<unknown>")

        val result = SongMetadataRepository.applyResolved(
            songs = listOf(song),
            id = 99,
            artist = "Başka Sanatçı",
            artworkPath = "/art/99.jpg"
        )

        result.single() shouldBe song
    }
})

private fun song(id: Long, artist: String, artworkPath: String? = null) = AudioFile(
    id = id,
    title = "Şarkı $id",
    artist = artist,
    duration = 180_000L,
    uri = mockk<Uri>(relaxed = true),
    albumId = 0L,
    folderName = "Music",
    dateAdded = 0L,
    artworkPath = artworkPath
)
