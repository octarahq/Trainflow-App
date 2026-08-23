package com.octarahq.trainflow.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.octarahq.trainflow.MainActivity
import com.octarahq.trainflow.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GithubRelease(
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    val published_at: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

data class GithubAsset(
    val name: String = "",
    val content_type: String? = null,
    val browser_download_url: String = "",
    val size: Long = 0
)

object UpdateManager {
    private const val NOTIFICATION_CHANNEL_ID = "trainflow_updates_channel"
    private const val NOTIFICATION_ID = 9988

    private val VERSION_REGEX = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?.*""", RegexOption.IGNORE_CASE)

    private suspend fun getLatestRelease(): GithubRelease = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/repos/octarahq/Trainflow-App/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to get release: ${connection.responseCode}")
        }

        val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObj = JSONObject(jsonStr)

        val tagName = jsonObj.optString("tag_name", "")
        val name = jsonObj.optString("name", null)
        val body = jsonObj.optString("body", null)
        val publishedAt = jsonObj.optString("published_at", null)

        val assetsArray = jsonObj.optJSONArray("assets")
        val assets = mutableListOf<GithubAsset>()
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.getJSONObject(i)
                assets.add(
                    GithubAsset(
                        name = assetObj.optString("name", ""),
                        content_type = assetObj.optString("content_type", null),
                        browser_download_url = assetObj.optString("browser_download_url", ""),
                        size = assetObj.optLong("size", 0)
                    )
                )
            }
        }

        GithubRelease(tagName, name, body, publishedAt, assets)
    }

    fun getInstalledVersion(context: Context): String? {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun isInstalledFromPlayStore(context: Context): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer == "com.android.vending"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkForUpdates(context: Context): GithubRelease? {
        if (isInstalledFromPlayStore(context)) return null

        return withContext(Dispatchers.IO) {
            try {
                val release = getLatestRelease()
                val tag = release.tag_name.trim()

                if (tag.isBlank() || !VERSION_REGEX.matches(tag)) return@withContext null

                val apkAsset = release.assets.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true) ||
                    it.browser_download_url.endsWith(".apk", ignoreCase = true) ||
                    it.content_type?.contains("package-archive", ignoreCase = true) == true
                } ?: return@withContext null

                val installedVersion = getInstalledVersion(context) ?: "1.0.0"

                if (compareVersions(tag, installedVersion) > 0) {
                    sendUpdateNotification(context, release, apkAsset.browser_download_url)
                    return@withContext release
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.trim().removePrefix("v").removePrefix("V")
        val cleanV2 = v2.trim().removePrefix("v").removePrefix("V")

        val parts1 = cleanV1.split(Regex("[.-]")).mapNotNull { it.toIntOrNull() }
        val parts2 = cleanV2.split(Regex("[.-]")).mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    private fun sendUpdateNotification(context: Context, release: GithubRelease, downloadUrl: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Mises à jour Trainflow",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouvelles mises à jour de Trainflow"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "update")
            putExtra("update_tag", release.tag_name)
            putExtra("update_notes", release.body ?: "")
            putExtra("update_apk_url", downloadUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mise à jour disponible : ${release.tag_name}")
            .setContentText("Une nouvelle version de Trainflow est disponible. Appuyez pour mettre à jour.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        tag: String,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        onError("Erreur de serveur (${connection.responseCode})")
                    }
                    return@withContext
                }

                val fileLength = connection.contentLength
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val apkFile = File(downloadDir, "Trainflow-${tag.replace('/', '_')}.apk")

                val input = connection.inputStream
                val output = FileOutputStream(apkFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    if (fileLength > 0) {
                        val progress = total.toFloat() / fileLength.toFloat()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile, tag)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("Échec du téléchargement : ${e.localizedMessage ?: "erreur inconnue"}")
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File, tag: String) {
        if (!apkFile.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
