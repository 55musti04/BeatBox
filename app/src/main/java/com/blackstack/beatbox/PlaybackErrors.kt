package com.blackstack.beatbox

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun Throwable.causeChain(): List<Throwable> = generateSequence(this) { it.cause }.toList()

fun playbackErrorMessages(
    isOnline: Boolean,
    offlineMessage: String,
    guidance: String?,
    rawErrorMessages: List<String>,
): List<String> {
    if (!isOnline) return listOf(offlineMessage)

    return buildList {
        guidance?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(rawErrorMessages.filter { it.isNotBlank() })
    }.distinct()
}

fun Context.hasUsableNetwork(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
