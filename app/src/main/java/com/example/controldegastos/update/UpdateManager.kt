package com.example.controldegastos.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val updateNotes: String
)

object UpdateManager {
    fun checkForUpdates(
        context: Context,
        onUpdateAvailable: (AppUpdateInfo) -> Unit
    ) {
        val currentCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("app_config").document("latest_version")
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val latestCode = doc.getLong("latestVersionCode")?.toInt() ?: 1
                    val latestName = doc.getString("latestVersionName") ?: "1.0"
                    val apkUrl = doc.getString("apkUrl") ?: ""
                    val notes = doc.getString("updateNotes") ?: "Nuevas mejoras y optimizaciones"

                    if (latestCode > currentCode && apkUrl.isNotBlank()) {
                        val info = AppUpdateInfo(
                            latestVersionCode = latestCode,
                            latestVersionName = latestName,
                            apkUrl = apkUrl,
                            updateNotes = notes
                        )
                        onUpdateAvailable(info)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpdateManager", "Error checking for updates", e)
            }
    }

    fun downloadAndInstallApk(context: Context, rawApkUrl: String) {
        Toast.makeText(context, "Abriendo página de descarga...", Toast.LENGTH_SHORT).show()
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rawApkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No se pudo abrir el enlace de descarga", Toast.LENGTH_SHORT).show()
        }
    }
}
