package com.example.data.api

import com.example.data.model.BmkgAutogempaResponse
import com.example.data.model.BmkgListResponse
import com.example.data.model.UsgsGeoJsonResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface BmkgApiService {
    @GET("autogempa.json")
    suspend fun getAutogempa(): BmkgAutogempaResponse

    @GET("gempaterkini.json")
    suspend fun getGempaTerkini(): BmkgListResponse

    @GET("gempadirasakan.json")
    suspend fun getGempaDirasakan(): BmkgListResponse
}

interface UsgsApiService {
    @GET("all_hour.geojson")
    suspend fun getAllHour(): UsgsGeoJsonResponse

    @GET("4.5_hour.geojson")
    suspend fun get45Hour(): UsgsGeoJsonResponse

    @GET("2.5_day.geojson")
    suspend fun get25Day(): UsgsGeoJsonResponse

    @GET("4.5_day.geojson")
    suspend fun get45Day(): UsgsGeoJsonResponse
}

object ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "GEMPAKU-Android-Monitor/1.0 (BMKG-USGS-Feed)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val bmkgApi: BmkgApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.bmkg.go.id/DataMKG/TEWS/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BmkgApiService::class.java)
    }

    val usgsApi: UsgsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UsgsApiService::class.java)
    }
}
