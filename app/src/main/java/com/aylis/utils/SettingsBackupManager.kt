package com.aylis.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SettingsBackupManager {

    private fun getSharedPrefsDir(context: Context): File {
        return File(context.applicationInfo.dataDir, "shared_prefs")
    }

    private fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    fun exportToStream(context: Context, outStream: OutputStream) {
        val prefsDir = getSharedPrefsDir(context)
        val filesDir = getFilesDir(context)

        val mainPrefsName = "${context.packageName}_preferences.xml"
        val ambientPrefsName = "ambient_settings.xml"

        val filesToBackup = mutableListOf<File>()

        // Add main prefs
        val mainPrefsFile = File(prefsDir, mainPrefsName)
        if (mainPrefsFile.exists()) {
            filesToBackup.add(mainPrefsFile)
        }

        // Add ambient prefs
        val ambientPrefsFile = File(prefsDir, ambientPrefsName)
        if (ambientPrefsFile.exists()) {
            filesToBackup.add(ambientPrefsFile)
        }

        // Add visualizer scenes
        val sceneFiles = filesDir.listFiles { _, name -> name.startsWith("vThemeScene_") && name.endsWith(".json") }
        if (sceneFiles != null) {
            filesToBackup.addAll(sceneFiles)
        }

        ZipOutputStream(outStream).use { zos ->
            for (file in filesToBackup) {
                // Determine zip entry name based on where it should be restored
                val entryName = if (file.name.endsWith(".xml")) {
                    "shared_prefs/${file.name}"
                } else {
                    "files/${file.name}"
                }

                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }

    fun importFromStream(context: Context, inputStream: InputStream): Boolean {
        val prefsDir = getSharedPrefsDir(context)
        val filesDir = getFilesDir(context)
        val mainPrefsName = "${context.packageName}_preferences.xml"

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val targetFile = when {
                        name.startsWith("shared_prefs/") -> {
                            if (!prefsDir.exists()) prefsDir.mkdirs()
                            File(prefsDir, name.substringAfter("shared_prefs/"))
                        }
                        name.startsWith("files/") -> {
                            if (!filesDir.exists()) filesDir.mkdirs()
                            File(filesDir, name.substringAfter("files/"))
                        }
                        else -> null
                    }

                    // Strict security check: only allow specific safe files
                    val isAllowed = targetFile != null && !name.contains("..") && (
                            targetFile.name == mainPrefsName ||
                            targetFile.name == "ambient_settings.xml" ||
                            (targetFile.name.startsWith("vThemeScene_") && targetFile.name.endsWith(".json"))
                    )

                    if (isAllowed && targetFile != null) {
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
