package com.lockpc.admin

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Models mirror the server's JSON

data class PcItem(
    val id: String?,
    val name: String?,
    val ip: String?,
    val status: String?,
    val connected: Boolean?
)

data class BlockPeriod(
    val id: Int,
    val from: String,
    val to: String,
    val days: List<String>?
)

data class BlockPeriodRequest(
    val from: String,
    val to: String,
    val days: List<String>
)

data class LoginTokenResponse(
    val token: String?
)

interface ApiService {
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api/login-token")
    suspend fun loginToken(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<LoginTokenResponse>

    @GET("api/block-period")
    suspend fun getBlockPeriods(): Response<List<BlockPeriod>>

    @GET("api/pcs")
    suspend fun getPcs(): Response<List<PcItem>>

    @POST("api/block-period")
    suspend fun createBlockPeriod(@Body body: BlockPeriodRequest): Response<BlockPeriod>

    @PUT("api/block-period/{id}")
    suspend fun updateBlockPeriod(
        @Path("id") id: Int,
        @Body body: BlockPeriodRequest
    ): Response<ResponseBody>

    @DELETE("api/block-period/{id}")
    suspend fun deleteBlockPeriod(@Path("id") id: Int): Response<ResponseBody>

    @GET("logout")
    suspend fun logout(): Response<ResponseBody>
}
