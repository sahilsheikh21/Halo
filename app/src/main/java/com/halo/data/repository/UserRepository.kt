package com.halo.data.repository

import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.UserEntity
import com.halo.data.matrix.MatrixClientManager
import com.halo.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val matrixClientManager: MatrixClientManager
) {
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
