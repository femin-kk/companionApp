package com.workout.tv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    
    @GET("api/state")
    suspend fun getState(): ServerState
    
    @POST("api/state/navigate")
    suspend fun navigate(@Body request: NavigateRequest): CurrentWorkout?
    
    @POST("api/state/set-group")
    suspend fun setGroup(@Body request: SetGroupRequest): Map<String, Boolean>
    
    @GET("api/groups")
    suspend fun getGroups(): List<WorkoutGroup>
    
    companion object {
        fun create(baseUrl: String): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}