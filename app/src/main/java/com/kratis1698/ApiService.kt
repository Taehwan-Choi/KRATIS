package com.kratis1698

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api297/getOpenDataList")
    suspend fun getOpenDataList(
        @Query("ServiceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numRows: Int,
        @Query("_type") _type: String,
    ): ResponseBody
}

interface ApiService2 {
    @GET("api298/getOpenDataList")
    suspend fun getOpenDataList(
        @Query("ServiceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numRows: Int,
        @Query("_type") _type: String,
    ): ResponseBody
}