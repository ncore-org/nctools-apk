package eu.nctools.app.data.api

import eu.nctools.app.data.api.auth.AuthApi
import eu.nctools.app.data.model.DeviceRegisterRequest
import eu.nctools.app.data.model.UpdateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * nctools backend API used by the Android app.
 * All authenticated requests are signed with the session bearer token via
 * AuthInterceptor. The backend is the same PostgreSQL + auth service that
 * powers nctools.eu.
 */
interface NctoolsApi {

    /** Register this install for update delivery + license/anti-crack. */
    @POST("api/mobile/devices")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): DeviceRegisterResponse

    /** Check whether a newer APK exists for this install. */
    @GET("api/mobile/updates")
    suspend fun checkUpdates(
        @Query("currentVersion") currentVersion: Int,
        @Query("platform") platform: String = "android",
    ): UpdateResponse

    /** Report a completed (ad-gated) tool conversion for analytics. */
    @POST("api/mobile/events")
    suspend fun reportEvent(@Body event: ReportEventRequest)

    data class DeviceRegisterResponse(
        val deviceId: String,
        val registered: Boolean,
        val minSupportedVersionCode: Int,
    )

    data class ReportEventRequest(
        val tool: String,
        val adShown: Boolean,
        val durationMs: Long,
        val deviceId: String? = null,
    )
}