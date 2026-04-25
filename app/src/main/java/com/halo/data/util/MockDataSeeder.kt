package com.halo.data.util

import com.halo.data.local.dao.ChatRoomDao
import com.halo.data.local.dao.PostDao
import com.halo.data.local.dao.StoryDao
import com.halo.data.local.dao.UserDao
import com.halo.data.local.entity.ChatRoomEntity
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.StoryEntity
import com.halo.data.local.entity.UserEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDataSeeder @Inject constructor(
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val chatRoomDao: ChatRoomDao
) {
    suspend fun seedIfNeeded() {
        if (userDao.getUserCount() > 0) return

        // 1. Seed Users
        val users = listOf(
            UserEntity("@halo_team:matrix.org", "Halo Team", "mxc://matrix.org/vAtpYqIu", "Official Halo updates", isFollowing = true),
            UserEntity("@sahil:matrix.org", "Sahil Sheikh", null, "Developer", isFollowing = true)
        )
        userDao.insertUsers(users)

        // 2. Seed Posts
        val posts = listOf(
            PostEntity(
                eventId = "seed_post_1",
                feedRoomId = "global_feed",
                authorId = "@halo_team:matrix.org",
                caption = "Welcome to Halo! 🚀 The future of decentralized social is here.",
                mediaJson = "[]",
                locationJson = null,
                tagsJson = "[\"welcome\", \"halo\"]",
                likeCount = 42,
                commentCount = 5,
                isLikedByMe = true,
                createdAt = System.currentTimeMillis() - 3600000
            )
        )
        postDao.insertPosts(posts)

        // 3. Seed Stories
        val stories = listOf(
            StoryEntity(
                eventId = "seed_story_1",
                feedRoomId = "global_feed",
                authorId = "@halo_team:matrix.org",
                mediaMxc = "mxc://matrix.org/vAtpYqIu",
                storyType = "image",
                durationMs = 5000,
                caption = "Testing stories...",
                createdAt = System.currentTimeMillis() - 1800000,
                isSeen = false
            )
        )
        storyDao.insertStories(stories)

        // 4. Seed Chat Rooms
        val rooms = listOf(
            ChatRoomEntity(
                roomId = "!halo_general:matrix.org",
                name = "Halo General",
                avatarUrl = null,
                lastMessage = "Hello everyone!",
                lastMessageAt = System.currentTimeMillis() - 600000,
                unreadCount = 2,
                isDm = false,
                membersJoined = "@halo_team:matrix.org,@sahil:matrix.org"
            )
        )
        chatRoomDao.insertChatRooms(rooms)
    }
}
