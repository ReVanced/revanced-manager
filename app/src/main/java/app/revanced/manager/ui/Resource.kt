package app.revanced.manager.ui

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()

    companion object {
        fun <T> success(value: T) = Success(value)
    }
}