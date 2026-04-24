package com.halo.data.repository

import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.UserEntity
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val matrixClientManager: MatrixClientManager
) {
    suspend fun refreshUsers() {
        // TODO: Sync following users from Matrix SDK and update local DB
    }
    fun observeUser(userId: String): Flow<UserProfile?> {
        return userDao.observeUser(userId).map { entity ->
            entity?.toDomainModel()
        }
    }

    fun getFollowingUsers(): Flow<List<UserProfile>> {
        return userDao.getFollowingUsers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Search for users globally via the Matrix homeserver.
     * If query starts with @, tries to fetch that specific user.
     * Otherwise, uses the homeserver's user directory search.
     */
    suspend fun searchUsersReal(query: String): Result<List<UserProfile>> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val client = matrixClientManager.getClient() 
            ?: return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            // If it's a full User ID, we can try to fetch profile directly
            if (query.startsWith("@") && query.contains(":")) {
                val profile = client.getProfile(query)
                return@withContext Result.success(listOf(
                    UserProfile(
                        userId = query,
                        displayName = profile.displayName ?: "",
                        avatarUrl = profile.avatarUrl,
                        isCurrentUser = query == matrixClientManager.getCurrentSession()?.userId
                    )
                ))
            }

            // Otherwise, use user directory search
            // SDK: client.searchUsers(query, limit = 50)
            // Note: In version 26.03.31, the exact method might be on the client
            val results = client.searchUsers(query, 50u)
            val users = results.results.map { res ->
                UserProfile(
                    userId = res.userId,
                    displayName = res.displayName ?: "",
                    avatarUrl = res.avatarUrl,
                    isCurrentUser = res.userId == matrixClientManager.getCurrentSession()?.userId
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchUsers(query: String): Flow<List<UserProfile>> {
        return userDao.searchUsers(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return userDao.getUserById(userId)?.toDomainModel()
    }

    suspend fun followUser(userId: String) {
        // TODO: Join the user's Feed Room via Matrix SDK
        userDao.updateFollowState(userId, true)
    }

    suspend fun unfollowUser(userId: String) {
        // TODO: Leave the user's Feed Room via Matrix SDK
        userDao.updateFollowState(userId, false)
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val session = matrixClientManager.getCurrentSession() ?: return null
        return getUserProfile(session.userId)
    }

    private fun UserEntity.toDomainModel(): UserProfile {
        return UserProfile(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarMxc, // TODO: resolve mxc to http
            bio = bio,
            feedRoomId = feedRoomId,
            isFollowing = isFollowing,
            followerCount = followerCount,
            followingCount = followingCount,
            postCount = postCount,
            isCurrentUser = userId == matrixClientManager.getCurrentSession()?.userId
        )
    }
}
