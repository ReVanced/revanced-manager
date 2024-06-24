package app.revanced.manager.patcher.logger

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

abstract class Logger {
    abstract fun log(level: LogLevel, message: String)

    fun trace(msg: String) = log(LogLevel.TRACE, msg)
    fun info(msg: String) = log(LogLevel.INFO, msg)
    fun warn(msg: String) = log(LogLevel.WARN, msg)
    fun error(msg: String) = log(LogLevel.ERROR, msg)

    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            val msg = record.message

            when (record.level) {
                Level.INFO -> info(msg)
                Level.SEVERE -> error(msg)
                Level.WARNING -> warn(msg)
                else -> trace(msg)
            }
        }

        override fun flush() = Unit
        override fun close() = Unit
    }
}

enum class LogLevel {
    TRACE,
    INFO,
    WARN,
    ERROR,
}