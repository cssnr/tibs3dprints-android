package org.cssnr.tibs3dprints.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.cssnr.tibs3dprints.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


class ServerApi(val context: Context) {

    val api: ApiService

    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val userAgent = "${context.packageName}/${versionName}"

    init {
        api = createRetrofit().create(ApiService::class.java)
    }

    //suspend fun loginUser(code: String, codeVerifier: String): Response<Unit> {
    //    Log.d("loginUser", "code: $code")
    //    Log.d("loginUser", "codeVerifier: $codeVerifier")
    //    val authRequest = AuthRequest(code = code, codeVerifier = codeVerifier)
    suspend fun serverLogin(authRequest: ServerAuthRequest): Response<LoginResponse> {
        Log.d("serverLogin", "authRequest: $authRequest")
        return try {
            api.login(authRequest)
        } catch (e: Exception) {
            val errorBody = e.toString().toResponseBody("text/plain".toMediaTypeOrNull())
            Response.error(520, errorBody)
        }
    }

    @JsonClass(generateAdapter = true)
    data class ServerAuthRequest(
        @Json(name = "code")
        val code: String,
        @Json(name = "codeVerifier")
        val codeVerifier: String,
    )

    @JsonClass(generateAdapter = true)
    data class LoginResponse(
        @Json(name = "display_name")
        val displayName: String,
        @Json(name = "open_id")
        val openId: String,
        @Json(name = "union_id")
        val unionId: String,
        @Json(name = "avatar_url")
        val avatarUrl: String
    )


    interface ApiService {
        @POST(("auth/"))
        suspend fun login(
            @Body authRequest: ServerAuthRequest
        ): Response<LoginResponse>
    }

    private fun createRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
                chain.proceed(request)
            }
            .build()
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.APP_API_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }
}
