package com.halo.data.repository

import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.UserEntity
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.UserProfile
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
        // Temporarily seed MockData if DB is empty
        val currentUsers = userDao.getFollowingUsers().first()
        if (currentUsers.isEmpty()) {
            val mockEntities = com.halo.data.mock.MockData.users.map { user ->
                UserEntity(
                    userId = user.userId,
                    displayName = user.displayName,
                    avatarMxc = user.avatarUrl, // HTTP URL for now
                    bio = user.bio,
                    feedRoomId = user.feedRoomId ?: "mock_feed_room",
                    isFollowing = user.isFollowing,
                    followerCount = user.followerCount,
                    followingCount = user.followingCount,
                    postCount = user.postCount,
                    cachedAt = System.currentTimeMillis()
                )
            }
            userDao.insertUsers(mockEntities)
        }
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
