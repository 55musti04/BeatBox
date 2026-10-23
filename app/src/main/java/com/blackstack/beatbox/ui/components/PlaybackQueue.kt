package com.blackstack.beatbox.ui.components

import com.blackstack.beatbox.AudioFile

/**
 * Kuyruktaki bir sonraki şarkıyı seçer.
 *
 * Karıştırma açıkken mevcut şarkı hariç rastgele bir parça seçilir; kapalıyken
 * listedeki sıradaki parça, sona gelindiyse listenin ilk parçası döner.
 */
fun nextSongInQueue(
    queue: List<AudioFile>,
    currentId: Long?,
    shuffle: Boolean
): AudioFile? {
    if (queue.isEmpty()) return null
    if (shuffle) {
        val available = queue.filter { it.id != currentId }
        return if (available.isNotEmpty()) available.random() else queue.random()
    }
    val index = queue.indexOfFirst { it.id == currentId }
    return if (index >= 0 && index < queue.lastIndex) queue[index + 1] else queue.first()
}

/**
 * Kuyruktaki bir önceki şarkıyı seçer.
 *
 * Karıştırma açıkken [nextSongInQueue] ile aynı rastgele seçim yapılır;
 * kapalıyken listenin başına gelindiğinde son parçaya dönülür.
 */
fun previousSongInQueue(
    queue: List<AudioFile>,
    currentId: Long?,
    shuffle: Boolean
): AudioFile? {
    if (queue.isEmpty()) return null
    if (shuffle) {
        val available = queue.filter { it.id != currentId }
        return if (available.isNotEmpty()) available.random() else queue.random()
    }
    val index = queue.indexOfFirst { it.id == currentId }
    return if (index > 0) queue[index - 1] else queue.last()
}
