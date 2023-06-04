package app.revanced.manager.util.signing

data class SigningOptions(
    val cn: String,
    val password: String,
    val keyStoreFilePath: String
)