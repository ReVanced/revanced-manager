package app.revanced.manager.patcher.logger

import app.revanced.manager.patcher.ProgressEvent
import app.revanced.manager.patcher.StepId
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

abstract class Logger {
    abstract fun log(level: LogLevel, message: String)

    // Logger-name metadata is only used for step-view filtering; default behavior keeps full logs.
    open fun log(level: LogLevel, message: String, loggerName: String?) = log(level, message)

    fun trace(msg: String) = log(LogLevel.TRACE, msg)
    fun info(msg: String) = log(LogLevel.INFO, msg)
    fun warn(msg: String) = log(LogLevel.WARN, msg)
    fun error(msg: String) = log(LogLevel.ERROR, msg)

    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            val msg = record.message
            val loggerName = record.loggerName

            when (record.level) {
                Level.INFO -> log(LogLevel.INFO, msg, loggerName)
                Level.SEVERE -> log(LogLevel.ERROR, msg, loggerName)
                Level.WARNING -> log(LogLevel.WARN, msg, loggerName)
                else -> log(LogLevel.TRACE, msg, loggerName)
            }
        }

        override fun flush() = Unit
        override fun close() = Unit
    }
}

fun Logger.forStep(stepId: StepId, minLogLevel: LogLevel, onEvent: (ProgressEvent) -> Unit) = object : Logger() {
    override fun log(level: LogLevel, message: String) {
        this@forStep.log(level, message)
        if (level.ordinal >= minLogLevel.ordinal) {
            onEvent(ProgressEvent.Log(stepId, level, message))
        }
    }

    override fun log(level: LogLevel, message: String, loggerName: String?) {
        this@forStep.log(level, message, loggerName)

        // App loggers should use empty or "app.revanced" prefix;
        // filter out logs from libraries to avoid cluttering the step view.
        if (level.ordinal >= minLogLevel.ordinal &&
            (loggerName.isNullOrEmpty() || loggerName.startsWith("app.revanced"))) {
            onEvent(ProgressEvent.Log(stepId, level, message))
        }
    }
}

inline fun <T> Logger.withJavaLogging(block: () -> T): T {
    val rootLogger = java.util.logging.Logger.getLogger("")
    
    // Save the previous level and force INFO to prevent the library from 
    // eagerly allocating millions of string/LogRecord objects for TRACE logs.
    val previousLevel = rootLogger.level
    rootLogger.level = Level.INFO

    val oldHandlers = rootLogger.handlers.toList()
    oldHandlers.forEach {
        it.close()
        rootLogger.removeHandler(it)
    }

    rootLogger.addHandler(handler)

    return try {
        block()
    } finally {
        rootLogger.removeHandler(handler)
        rootLogger.level = previousLevel
    }
}

enum class LogLevel {
    TRACE,
    INFO,
    WARN,
    ERROR,
}