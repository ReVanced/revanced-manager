package app.revanced.manager.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReVancedInfo(
    val name: String,
    val about: String,
    val keys: String,
    val branding: ReVancedBranding,
    val status: String,
    val contact: ReVancedContact,
    val socials: List<ReVancedSocial>,
    val donations: ReVancedDonation,
)

@Serializable
data class ReVancedBranding(
    val logo: String,
)

@Serializable
data class ReVancedContact(
    val email: String,
)

@Serializable
data class ReVancedSocial(
    val name: String,
    val url: String,
    val preferred: Boolean = false,
)

@Serializable
data class ReVancedDonation(
    val wallets: List<ReVancedWallet>,
    val links: List<ReVancedDonationLink>,
)

@Serializable
data class ReVancedWallet(
    val network: String,
    @SerialName("currency_code")
    val currencyCode: String,
    val address: String,
    val preferred: Boolean = false
)

@Serializable
data class ReVancedDonationLink(
    val name: String,
    val url: String,
    val preferred: Boolean = false,
)
