package app.revanced.manager.patcher.logger

import android.util.Log
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class ManagerLogger : Handler() {
    private val logs = mutableListOf<Pair<LogLevel, String>>()
    private fun log(level: LogLevel, msg: String) {
        level.androidLog(msg)
        if (level == LogLevel.TRACE) return
        logs.add(level to msg)
    }

    fun export() =
        logs.asSequence().map { (level, msg) -> "[${level.name}]: $msg" }.joinToString("\n")

    fun trace(msg: String) = log(LogLevel.TRACE, msg)
    fun info(msg: String) = log(LogLevel.INFO, msg)
    fun warn(msg: String) = log(LogLevel.WARN, msg)
    fun error(msg: String) = log(LogLevel.ERROR, msg)
    override fun publish(record: LogRecord) {
        val msg = record.message
        val fn = when (record.level) {
            Level.INFO -> ::info
            Level.SEVERE -> ::error
            Level.WARNING -> ::warn
            else -> ::trace
        }

        fn(msg)
    }

    override fun flush() = Unit

    override fun close() = Unit
}

enum class LogLevel {
    TRACE {
        override fun androidLog(msg: String) = Log.v(androidTag, msg)
    },
    INFO {
        override fun androidLog(msg: String) = Log.i(androidTag, msg)
    },
    WARN {
        override fun androidLog(msg: String) = Log.w(androidTag, msg)
    },
    ERROR {
        override fun androidLog(msg: String) = Log.e(androidTag, msg)
    };

    abstract fun androidLog(msg: String): Int

    private companion object {
        const val androidTag = "ReVanced Patcher"
    }
}