package eu.nctools.app.data.model

import com.squareup.moshi.Json

/** User account DTO shared with the nctools.eu backend. */
data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "emailVerified") val emailVerified: Boolean = false,
)

/** Payload sent when this install registers with the update/anti-crack API. */
data class DeviceRegisterRequest(
    val deviceId: String,
    val appVersionCode: Int,
    val appVersionName: String,
    /** device attestation / install token (tamper check) */
    val installToken: String,
    val packageName: String,
)

/** Update-channel response from the VPS backend. */
data class UpdateResponse(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minSupportedVersionCode: Int,
    /** signed, URL-derived APK descriptor; the client verifies the signature */
    val apk: ApkDescriptor? = null,
    val releaseNotes: List<String> = emptyList(),
    val enforced: Boolean = false,
)

data class ApkDescriptor(
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
)