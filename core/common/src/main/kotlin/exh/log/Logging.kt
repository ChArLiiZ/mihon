package exh.log

import android.util.Log

/**
 * Simplified logging utilities replacing XLog dependency.
 * Uses Android's built-in Log class.
 */
fun Any.xLog(): AndroidLogger = AndroidLogger(this::class.java.simpleName)

fun Any.xLogE(log: String) = Log.e(this::class.java.simpleName, log)
fun Any.xLogW(log: String) = Log.w(this::class.java.simpleName, log)
fun Any.xLogD(log: String) = Log.d(this::class.java.simpleName, log)
fun Any.xLogI(log: String) = Log.i(this::class.java.simpleName, log)

fun Any.xLogE(log: String, e: Throwable) = Log.e(this::class.java.simpleName, log, e)
fun Any.xLogW(log: String, e: Throwable) = Log.w(this::class.java.simpleName, log, e)
fun Any.xLogD(log: String, e: Throwable) = Log.d(this::class.java.simpleName, log, e)
fun Any.xLogI(log: String, e: Throwable) = Log.i(this::class.java.simpleName, log, e)

fun Any.xLogE(log: Any?) = Log.e(this::class.java.simpleName, log?.toString() ?: "null")
fun Any.xLogW(log: Any?) = Log.w(this::class.java.simpleName, log?.toString() ?: "null")
fun Any.xLogD(log: Any?) = Log.d(this::class.java.simpleName, log?.toString() ?: "null")
fun Any.xLogI(log: Any?) = Log.i(this::class.java.simpleName, log?.toString() ?: "null")

fun Any.xLogE(format: String, vararg args: Any?) = Log.e(this::class.java.simpleName, String.format(format, *args))
fun Any.xLogW(format: String, vararg args: Any?) = Log.w(this::class.java.simpleName, String.format(format, *args))
fun Any.xLogD(format: String, vararg args: Any?) = Log.d(this::class.java.simpleName, String.format(format, *args))
fun Any.xLogI(format: String, vararg args: Any?) = Log.i(this::class.java.simpleName, String.format(format, *args))

@Deprecated("Use proper throwable function", ReplaceWith("""xLogE("", log)"""))
fun Any.xLogE(log: Throwable) = Log.e(this::class.java.simpleName, "", log)

@Deprecated("Use proper throwable function", ReplaceWith("""xLogW("", log)"""))
fun Any.xLogW(log: Throwable) = Log.w(this::class.java.simpleName, "", log)

@Deprecated("Use proper throwable function", ReplaceWith("""xLogD("", log)"""))
fun Any.xLogD(log: Throwable) = Log.d(this::class.java.simpleName, "", log)

@Deprecated("Use proper throwable function", ReplaceWith("""xLogI("", log)"""))
fun Any.xLogI(log: Throwable) = Log.i(this::class.java.simpleName, "", log)

/**
 * Simple wrapper around Android Log to provide a Logger-like API
 */
class AndroidLogger(private val tag: String) {
    fun d(msg: String) = Log.d(tag, msg)
    fun d(msg: Any?) = Log.d(tag, msg?.toString() ?: "null")
    fun e(msg: String) = Log.e(tag, msg)
    fun e(msg: String, t: Throwable) = Log.e(tag, msg, t)
    fun w(msg: String) = Log.w(tag, msg)
    fun w(msg: String, t: Throwable) = Log.w(tag, msg, t)
    fun i(msg: String) = Log.i(tag, msg)
    fun i(msg: String, t: Throwable) = Log.i(tag, msg, t)
}
