@file:Suppress("NOTHING_TO_INLINE")

package app.revanced.manager.network.utils

import io.ktor.http.*

/**
 * @author Aliucord Authors, DiamondMiner88
 */

sealed interface APIResponse<T> {
    data class Success<T>(val data: T) : APIResponse<T>
    data class Error<T>(val error: APIError) : APIResponse<T>
    data class Failure<T>(val error: APIFailure) : APIResponse<T>
}

class APIError(code: HttpStatusCode, body: String?) : Exception("HTTP Code $code, Body: $body")

class APIFailure(error: Throwable, body: String?) : Exception(body ?: error.message, error)

inline fun <T, R> APIResponse<T>.fold(
    success: (T) -> R,
    error: (APIError) -> R,
    failure: (APIFailure) -> R
): R {
    return when (this) {
        is APIResponse.Success -> success(this.data)
        is APIResponse.Error -> error(this.error)
        is APIResponse.Failure -> failure(this.error)
    }
}

inline fun <T, R> APIResponse<T>.fold(
    success: (T) -> R,
    fail: (Exception) -> R,
): R {
    return when (this) {
        is APIResponse.Success -> success(data)
        is APIResponse.Error -> fail(error)
        is APIResponse.Failure -> fail(error)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> APIResponse<T>.transform(block: (T) -> R): APIResponse<R> {
    return if (this !is APIResponse.Success) {
        // Error and Failure do not use the generic value
        this as APIResponse<R>
    } else {
        APIResponse.Success(block(data))
    }
}

inline fun <T> APIResponse<T>.getOrThrow(): T {
    return fold(
        success = { it },
        fail = { throw it }
    )
}

inline fun <T> APIResponse<T>.getOrNull(): T? {
    return fold(
        success = { it },
        fail = { null }
    )
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> APIResponse<T>.chain(block: (T) -> APIResponse<R>): APIResponse<R> {
    return if (this !is APIResponse.Success) {
        // Error and Failure do not use the generic value
        this as APIResponse<R>
    } else {
        block(data)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> APIResponse<T>.chain(secondary: APIResponse<R>): APIResponse<R> {
    return if (secondary is APIResponse.Success) {
        secondary
    } else {
        // Error and Failure do not use the generic value
        this as APIResponse<R>
    }
}