package org.cssnr.tibs3dprints.api

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
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
import retrofit2.http.GET
import retrofit2.http.POST


class ServerApi(val context: Context) {

    val api: ApiService
    val retrofit: Retrofit

    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val userAgent = "${context.packageName}/${versionName}"
    val authToken =
        PreferenceManager.getDefaultSharedPreferences(context).getString("authorization", null)
            ?: ""

    init {
        retrofit = createRetrofit()
        api = retrofit.create(ApiService::class.java)
    }

    //suspend fun loginUser(code: String, codeVerifier: String): Response<Unit> {
    //    Log.d("loginUser", "code: $code")
    //    Log.d("loginUser", "codeVerifier: $codeVerifier")
    //    val authRequest = AuthRequest(code = code, codeVerifier = codeVerifier)
    suspend fun tikTokLogin(tikTokAuthRequest: TikTokAuthRequest): Response<TikTokLoginResponse> {
        Log.d("tikTokLogin", "tikTokAuthRequest: $tikTokAuthRequest")
        return try {
            api.login(tikTokAuthRequest)
        } catch (e: Exception) {
            Log.e("tikTokLogin", e.stackTraceToString())
            val errorBody = e.toString().toResponseBody("text/plain".toMediaTypeOrNull())
            Response.error(520, errorBody)
        }
    }

    suspend fun getCurrentPoll(): PollResponse? {
        return api.getPollCurrent().takeIf { it.isSuccessful }?.body()
    }

    suspend fun submitVote(poll: Int, choice: Int): Vote? {
        val voteRequest = VoteRequest(poll = poll, choice = choice)
        return api.postVote(voteRequest).takeIf { it.isSuccessful }?.body()
    }

    suspend fun startLogin(email: String, state: String): Response<Unit> {
        return api.authStart(StartLoginRequest(email, state))
    }

    suspend fun verifyLogin(email: String, state: String, code: String): Response<UserResponse> {
        return api.authLogin(VerifyLoginRequest(email, state, code))
    }

    suspend fun getUser(): Response<UserResponse> {
        return api.userCurrent()
    }

    suspend fun editUser(editUserRequest: EditUserRequest): UserResponse? {
        val response = api.userEdit(editUserRequest)
        return if (response.isSuccessful) response.body() else null
    }

    @JsonClass(generateAdapter = true)
    data class PollResponse(
        @Json(name = "poll") val poll: Poll,
        @Json(name = "choices") val choices: List<Choice>,
        @Json(name = "vote") var vote: Vote? = null
    )

    @JsonClass(generateAdapter = true)
    data class Poll(
        @Json(name = "id") val id: Int,
        @Json(name = "title") val title: String,
        @Json(name = "question") val question: String,
        @Json(name = "start_at") val startAt: String,
        @Json(name = "end_at") val endAt: String,
    )

    @JsonClass(generateAdapter = true)
    data class Choice(
        @Json(name = "id") val id: Int,
        @Json(name = "poll") val poll: Int,
        @Json(name = "name") val name: String,
        @Json(name = "file") val file: String?,
        @Json(name = "votes") val votes: Int,
    )

    @JsonClass(generateAdapter = true)
    data class Vote(
        @Json(name = "id") val id: Int,
        @Json(name = "user_id") val userId: Int,
        @Json(name = "poll_id") val pollId: Int,
        @Json(name = "choice_id") val choiceId: Int,
        @Json(name = "notify_on_result") val notifyOnResult: Boolean,
        @Json(name = "voted_at") val votedAt: String?,
    )


    @JsonClass(generateAdapter = true)
    data class VoteRequest(
        @Json(name = "poll") val poll: Int,
        @Json(name = "choice") val choice: Int,
    )

    //@JsonClass(generateAdapter = true)
    //data class MessageResponse(
    //    @Json(name = "message") val message: String,
    //)

    @JsonClass(generateAdapter = true)
    data class TikTokAuthRequest(
        @Json(name = "code") val code: String,
        @Json(name = "codeVerifier") val codeVerifier: String,
    )

    @JsonClass(generateAdapter = true)
    data class TikTokLoginResponse(
        @Json(name = "display_name") val displayName: String,
        @Json(name = "avatar_url") val avatarUrl: String,
        @Json(name = "authorization") val authorization: String,
        @Json(name = "open_id") val openId: String,
        @Json(name = "union_id") val unionId: String,
    )

    @JsonClass(generateAdapter = true)
    data class StartLoginRequest(
        @Json(name = "email") val email: String,
        @Json(name = "state") val state: String,
    )

    @JsonClass(generateAdapter = true)
    data class VerifyLoginRequest(
        @Json(name = "email") val email: String,
        @Json(name = "state") val state: String,
        @Json(name = "code") val code: String,
    )

    @JsonClass(generateAdapter = true)
    data class UserResponse(
        @Json(name = "email") val email: String,
        @Json(name = "name") val name: String,
        @Json(name = "authorization") val authorization: String,
        @Json(name = "verified") val verified: Boolean,
        @Json(name = "points") val points: Int,
    )

    @JsonClass(generateAdapter = true)
    data class EditUserRequest(
        @Json(name = "name") val name: String,
    )

    @JsonClass(generateAdapter = true)
    data class ErrorResponse(val message: String)


    interface ApiService {
        @POST(("auth/"))
        suspend fun login(
            @Body data: TikTokAuthRequest,
        ): Response<TikTokLoginResponse>

        @POST(("auth/start/"))
        suspend fun authStart(
            @Body data: StartLoginRequest,
        ): Response<Unit>

        @POST(("auth/login/"))
        suspend fun authLogin(
            @Body data: VerifyLoginRequest,
        ): Response<UserResponse>

        @GET("user/current/")
        suspend fun userCurrent(): Response<UserResponse>

        @POST("user/edit/")
        suspend fun userEdit(
            @Body data: EditUserRequest,
        ): Response<UserResponse>

        @GET("poll/current/")
        suspend fun getPollCurrent(): Response<PollResponse?>

        @POST("poll/vote/")
        suspend fun postVote(@Body voteRequest: VoteRequest): Response<Vote>
    }

    private fun createRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Authorization", authToken)
                    .build()
                chain.proceed(request)
            }
            .build()
        val moshi = Moshi.Builder().build()
        val url = "${BuildConfig.APP_API_URL}/api/"
        Log.d("createRetrofit", "url: $url")
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }
}

fun <T> Response<*>.parseErrorBody(retrofit: Retrofit, type: Class<T>): T? {
    val errorBody = this.errorBody() ?: return null
    val converter = retrofit.responseBodyConverter<T>(type, emptyArray())
    return try {
        converter.convert(errorBody)
    } catch (e: Exception) {
        Log.e("parseErrorBody", "Failed to parse error body", e)
        null
    }
}
