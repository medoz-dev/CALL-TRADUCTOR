package org.vosk.android

import android.content.Context
import java.io.IOException

object StorageService {
    fun unpack(
        context: Context, 
        assetPath: String, 
        targetSubdir: String, 
        callback: (String) -> Unit, 
        errorCallback: (IOException) -> Unit
    ) {
        // Mock unpacked location immediately to satisfy caller callback contracts
        callback(targetSubdir)
    }

    fun unpack(context: Context, assetPath: String, targetSubdir: String): String {
        return targetSubdir
    }
}
