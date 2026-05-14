package com.zutm.app_mobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import app.tauri.annotation.ActivityCallback
import app.tauri.annotation.Command
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.Plugin
import app.tauri.plugin.JSObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@TauriPlugin
class MediaPlugin(private val activity: Activity) : Plugin(activity) {
    private var currentPath: String? = null

    @Command
    fun takePhoto(invoke: Invoke) {
        try {
            val photoFile = createImageFile()
            currentPath = photoFile.absolutePath
            val photoURI: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            startActivityForResult(invoke, takePictureIntent, "onCaptureResult")
        } catch (e: Exception) {
            invoke.reject("Failed to start camera: ${e.message}")
        }
    }

    @Command
    fun takeVideo(invoke: Invoke) {
        try {
            val videoFile = createVideoFile()
            currentPath = videoFile.absolutePath
            val videoURI: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                videoFile
            )

            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            startActivityForResult(invoke, takeVideoIntent, "onCaptureResult")
        } catch (e: Exception) {
            invoke.reject("Failed to start video camera: ${e.message}")
        }
    }

    @ActivityCallback
    fun onCaptureResult(invoke: Invoke, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val path = currentPath
            if (path != null) {
                val response = JSObject()
                response.put("path", path)
                // Determinamos el tipo basándonos en la extensión ya que el intent no lo dice directamente de forma fácil aquí
                response.put("type", if (path.endsWith(".jpg")) "image" else "video")
                invoke.resolve(response)
            } else {
                invoke.reject("File path was null")
            }
        } else {
            invoke.reject("Capture cancelled or failed")
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = activity.cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun createVideoFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = activity.cacheDir
        return File.createTempFile("VIDEO_${timeStamp}_", ".mp4", storageDir)
    }
}
