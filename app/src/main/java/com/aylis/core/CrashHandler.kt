package com.aylis.core

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            saveCrashLog(exception)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Always let the default handler process the crash so the OS knows about it
            defaultHandler?.uncaughtException(thread, exception) ?: exitProcess(1)
        }
    }

    private fun saveCrashLog(exception: Throwable) {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        val logContent = """
            Crash Log ($time)
            --- Device Info ---
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            
            --- Exception ---
            ${stackTrace.toString()}
        """.trimIndent()
        
        val logFile = File(context.filesDir, CRASH_LOG_FILE_NAME)
        logFile.writeText(logContent)
    }

    companion object {
        const val CRASH_LOG_FILE_NAME = "last_crash_log.txt"

        fun init(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
            }
        }
    }
}
