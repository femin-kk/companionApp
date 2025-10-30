package com.workout.tv.data

import com.google.gson.annotations.SerializedName

data class Workout(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("source_url") val sourceUrl: String?,
    @SerializedName("local_filename") val localFilename: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    val status: String
)

data class WorkoutGroup(
    val id: Int,
    val name: String,
    val workouts: List<GroupWorkout>
)

data class GroupWorkout(
    val id: Int,
    val name: String,
    val status: String,
    val position: Int
)

data class ServerState(
    @SerializedName("current_group_id") val currentGroupId: Int?,
    @SerializedName("current_index") val currentIndex: Int,
    @SerializedName("current_workout") val currentWorkout: CurrentWorkout?,
    @SerializedName("pairing_code") val pairingCode: String
)

data class CurrentWorkout(
    val id: Int,
    val name: String,
    val status: String,
    @SerializedName("video_url") val videoUrl: String?
)

data class NavigateRequest(
    val direction: String
)

data class SetGroupRequest(
    @SerializedName("group_id") val groupId: Int
)