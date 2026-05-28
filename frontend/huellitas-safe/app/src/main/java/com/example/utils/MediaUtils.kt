package com.example.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaUtils {

    fun createPhotoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null) ?: context.cacheDir
        return File.createTempFile("PET_IMG_${timeStamp}_", ".jpg", storageDir)
    }

    fun createVideoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null) ?: context.cacheDir
        return File.createTempFile("PET_VID_${timeStamp}_", ".mp4", storageDir)
    }
    
    // Genera URLs de imágenes simuladas de mascotas (hermosos perros y gatos) para que la experiencia en el emulador sea espectacular.
    fun getMockPetImage(index: Int): String {
        val mockImages = listOf(
            "https://images.unsplash.com/photo-1543466835-00a7907e9de1?q=80&w=350", // Perro feliz
            "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?q=80&w=350", // Gato tierno
            "https://images.unsplash.com/photo-1477884213980-b111f22879ef?q=80&w=350", // Perrito tierno
            "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?q=80&w=350", // Golden retriever
            "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?q=80&w=350", // Gato con lentes
            "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?q=80&w=350", // Perro listo
            "https://images.unsplash.com/photo-1574158622643-69d34ad29a0b?q=80&w=350", // Gato dormido
            "https://images.unsplash.com/photo-1596492784531-6e6eb5ea9993?q=80&w=350", // Cachorrito
            "https://images.unsplash.com/photo-1517849845537-4d257902454a?q=80&w=350", // Pug gracioso
            "https://images.unsplash.com/photo-1535268647977-a403b69fc756?q=80&w=350"  // Lobo místico
        )
        return mockImages[index % mockImages.size]
    }
}
