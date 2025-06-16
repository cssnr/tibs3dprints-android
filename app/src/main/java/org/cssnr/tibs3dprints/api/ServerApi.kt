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

    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val userAgent = "${context.packageName}/${versionName}"
    val authToken =
        PreferenceManager.getDefaultSharedPreferences(context).getString("authorization", null)
            ?: ""

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

    suspend fun getCurrentPoll(): PollResponse? {
        return api.getPollCurrent()
    }

    suspend fun submitVote(poll: Int, choice: Int): Vote? {
        val voteRequest = VoteRequest(poll = poll, choice = choice)
        val response = api.postVote(voteRequest)
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                return body
            }
        }
        return null
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
        @Json(name = "duration") val duration: Int
    )

    @JsonClass(generateAdapter = true)
    data class Choice(
        @Json(name = "id") val id: Int,
        @Json(name = "poll") val poll: Int,
        @Json(name = "name") val name: String,
        @Json(name = "file") val file: String?,
        @Json(name = "votes") val votes: Int
    )

    @JsonClass(generateAdapter = true)
    data class Vote(
        @Json(name = "id") val id: Int,
        @Json(name = "user_id") val userId: Int,
        @Json(name = "poll_id") val pollId: Int,
        @Json(name = "choice_id") val choiceId: Int,
        @Json(name = "notify_on_result") val notifyOnResult: Boolean,
        @Json(name = "voted_at") val votedAt: String?
    )


    @JsonClass(generateAdapter = true)
    data class VoteRequest(
        @Json(name = "poll") val poll: Int,
        @Json(name = "choice") val choice: Int
    )

    @JsonClass(generateAdapter = true)
    data class MessageResponse(
        @Json(name = "message") val message: String
    )

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
        @Json(name = "avatar_url")
        val avatarUrl: String,
        @Json(name = "authorization")
        val authorization: String,
        @Json(name = "open_id")
        val openId: String,
        @Json(name = "union_id")
        val unionId: String,
    )

    interface ApiService {
        @POST(("auth/"))
        suspend fun login(
            @Body authRequest: ServerAuthRequest
        ): Response<LoginResponse>

        @GET("poll/current/")
        suspend fun getPollCurrent(): PollResponse?

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
