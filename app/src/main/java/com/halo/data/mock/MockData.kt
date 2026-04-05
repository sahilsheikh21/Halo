package com.halo.data.mock

import com.halo.domain.model.ChatRoom
import com.halo.domain.model.MediaItem
import com.halo.domain.model.Post
import com.halo.domain.model.Story
import com.halo.domain.model.StoryGroup
import com.halo.domain.model.UserProfile

/**
 * Static mock data for UI development and testing.
 * Uses public Unsplash images that work without an API key.
 */
object MockData {

    // ─── Users ──────────────────────────────────────────────────
    val users = listOf(
        UserProfile(
            userId = "@aurora:matrix.org",
            displayName = "Aurora Sky",
            avatarUrl = "https://i.pravatar.cc/150?img=47",
            bio = "✨ Chasing golden hour vibes",
            postCount = 94,
            followerCount = 12400,
            followingCount = 381
        ),
        UserProfile(
            userId = "@neon:matrix.org",
            displayName = "Neon Drift",
            avatarUrl = "https://i.pravatar.cc/150?img=12",
            bio = "Urban explorer 🏙️ // Tokyo • Seoul • NYC",
            postCount = 237,
            followerCount = 44820,
            followingCount = 512
        ),
        UserProfile(
            userId = "@solaris:matrix.org",
            displayName = "Solaris",
            avatarUrl = "https://i.pravatar.cc/150?img=32",
            bio = "Astrophotographer 🌌",
            postCount = 58,
            followerCount = 8900,
            followingCount = 210
        ),
        UserProfile(
            userId = "@prism:matrix.org",
            displayName = "Prism Studio",
            avatarUrl = "https://i.pravatar.cc/150?img=5",
            bio = "Digital artist // Breaking boundaries",
            postCount = 312,
            followerCount = 91200,
            followingCount = 143
        ),
        UserProfile(
            userId = "@wave:matrix.org",
            displayName = "Wave Rider",
            avatarUrl = "https://i.pravatar.cc/150?img=68",
            bio = "🏄 Ocean soul. Surf. Create. Repeat.",
            postCount = 145,
            followerCount = 27600,
            followingCount = 642
        ),
        UserProfile(
            userId = "@ember:matrix.org",
            displayName = "Ember Rose",
            avatarUrl = "https://i.pravatar.cc/150?img=25",
            bio = "Film photographer 📷 | Color grader",
            postCount = 183,
            followerCount = 19300,
            followingCount = 421
        )
    )

    // ─── Posts ──────────────────────────────────────────────────
    val posts = listOf(
        Post(
            eventId = "post_1",
            authorId = "@aurora:matrix.org",
            authorName = "Aurora Sky",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=47",
            caption = "Golden hour never disappoints 🌅 sometimes you just need to stop and breathe it all in",
            locationName = "Santorini, Greece",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                    mxcUri = "mxc://mock/1",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 2847,
            commentCount = 134,
            isLikedByMe = false,
            createdAt = System.currentTimeMillis() - 3_600_000
        ),
        Post(
            eventId = "post_2",
            authorId = "@neon:matrix.org",
            authorName = "Neon Drift",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=12",
            caption = "Tokyo never sleeps and honestly, neither do I 🏙️✨ The neon, the rain, the energy — this city hits different at 2am",
            locationName = "Shinjuku, Tokyo",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&q=80",
                    mxcUri = "mxc://mock/2a",
                    mimeType = "image/jpeg"
                ),
                MediaItem(
                    url = "https://images.unsplash.com/photo-1536098561742-ca998e48cbcc?w=800&q=80",
                    mxcUri = "mxc://mock/2b",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 11203,
            commentCount = 429,
            isLikedByMe = true,
            createdAt = System.currentTimeMillis() - 7_200_000
        ),
        Post(
            eventId = "post_3",
            authorId = "@solaris:matrix.org",
            authorName = "Solaris",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=32",
            caption = "Milky Way over the Sahara 🌌 15 sec exposure, f/2.8, ISO 3200. No filters. The universe is art.",
            locationName = "Sahara Desert",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=800&q=80",
                    mxcUri = "mxc://mock/3",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 6410,
            commentCount = 218,
            isLikedByMe = false,
            createdAt = System.currentTimeMillis() - 18_000_000
        ),
        Post(
            eventId = "post_4",
            authorId = "@prism:matrix.org",
            authorName = "Prism Studio",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=5",
            caption = "Color is emotion. New digital series dropping this week 🎨",
            locationName = "Los Angeles, CA",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80",
                    mxcUri = "mxc://mock/4",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 23900,
            commentCount = 871,
            isLikedByMe = false,
            createdAt = System.currentTimeMillis() - 86_400_000
        ),
        Post(
            eventId = "post_5",
            authorId = "@wave:matrix.org",
            authorName = "Wave Rider",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=68",
            caption = "Morning glass 🌊 nothing beats an empty lineup at sunrise. Paddle out.",
            locationName = "Pipeline, Hawaii",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=800&q=80",
                    mxcUri = "mxc://mock/5",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 9752,
            commentCount = 306,
            isLikedByMe = false,
            createdAt = System.currentTimeMillis() - 172_800_000
        ),
        Post(
            eventId = "post_6",
            authorId = "@ember:matrix.org",
            authorName = "Ember Rose",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=25",
            caption = "Autumn through Kodak Portra 400 🍂 there's something irreplaceable about film",
            locationName = "Kyoto, Japan",
            mediaUrls = listOf(
                MediaItem(
                    url = "https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=800&q=80",
                    mxcUri = "mxc://mock/6a",
                    mimeType = "image/jpeg"
                ),
                MediaItem(
                    url = "https://images.unsplash.com/photo-1590766740466-39fe019e3fad?w=800&q=80",
                    mxcUri = "mxc://mock/6b",
                    mimeType = "image/jpeg"
                ),
                MediaItem(
                    url = "https://images.unsplash.com/photo-1516655855035-d5215376c7a3?w=800&q=80",
                    mxcUri = "mxc://mock/6c",
                    mimeType = "image/jpeg"
                )
            ),
            likeCount = 4521,
            commentCount = 167,
            isLikedByMe = true,
            createdAt = System.currentTimeMillis() - 259_200_000
        )
    )

    // ─── Stories ─────────────────────────────────────────────────
    val storyGroups = listOf(
        StoryGroup(
            authorId = "@aurora:matrix.org",
            authorName = "aurora",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=47",
            stories = listOf(
                Story(
                    eventId = "story_1",
                    authorId = "@aurora:matrix.org",
                    authorName = "aurora",
                    mediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                    mxcUri = "mxc://mock/s1",
                    createdAt = System.currentTimeMillis() - 1_800_000
                )
            ),
            hasUnseenStories = true
        ),
        StoryGroup(
            authorId = "@neon:matrix.org",
            authorName = "neon",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=12",
            stories = listOf(
                Story(
                    eventId = "story_2",
                    authorId = "@neon:matrix.org",
                    authorName = "neon",
                    mediaUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800",
                    mxcUri = "mxc://mock/s2",
                    createdAt = System.currentTimeMillis() - 3_600_000
                )
            ),
            hasUnseenStories = true
        ),
        StoryGroup(
            authorId = "@solaris:matrix.org",
            authorName = "solaris",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=32",
            stories = emptyList(),
            hasUnseenStories = false
        ),
        StoryGroup(
            authorId = "@prism:matrix.org",
            authorName = "prism",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=5",
            stories = listOf(
                Story(
                    eventId = "story_3",
                    authorId = "@prism:matrix.org",
                    authorName = "prism",
                    mediaUrl = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
                    mxcUri = "mxc://mock/s3",
                    createdAt = System.currentTimeMillis() - 7_200_000
                )
            ),
            hasUnseenStories = true
        ),
        StoryGroup(
            authorId = "@wave:matrix.org",
            authorName = "wave",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=68",
            stories = emptyList(),
            hasUnseenStories = false
        ),
        StoryGroup(
            authorId = "@ember:matrix.org",
            authorName = "ember",
            authorAvatarUrl = "https://i.pravatar.cc/150?img=25",
            stories = listOf(
                Story(
                    eventId = "story_4",
                    authorId = "@ember:matrix.org",
                    authorName = "ember",
                    mediaUrl = "https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=800",
                    mxcUri = "mxc://mock/s4",
                    createdAt = System.currentTimeMillis() - 14_400_000
                )
            ),
            hasUnseenStories = true
        )
    )

    // ─── Chat Rooms ──────────────────────────────────────────────
    val chatRooms = listOf(
        ChatRoom(
            roomId = "!dm1:matrix.org",
            name = "Aurora Sky",
            avatarUrl = "https://i.pravatar.cc/150?img=47",
            lastMessage = "that sunset photo is 🔥 where was that?",
            lastMessageAt = System.currentTimeMillis() - 900_000,
            unreadCount = 2,
            isDm = true
        ),
        ChatRoom(
            roomId = "!dm2:matrix.org",
            name = "Neon Drift",
            avatarUrl = "https://i.pravatar.cc/150?img=12",
            lastMessage = "Tokyo collab? I'm here next month",
            lastMessageAt = System.currentTimeMillis() - 7_200_000,
            unreadCount = 0,
            isDm = true
        ),
        ChatRoom(
            roomId = "!dm3:matrix.org",
            name = "Prism Studio",
            avatarUrl = "https://i.pravatar.cc/150?img=5",
            lastMessage = "loved your latest series ✨",
            lastMessageAt = System.currentTimeMillis() - 86_400_000,
            unreadCount = 1,
            isDm = true
        ),
        ChatRoom(
            roomId = "!group1:matrix.org",
            name = "📸 Photographers Guild",
            avatarUrl = null,
            lastMessage = "ember: anyone using the new 50mm 1.2?",
            lastMessageAt = System.currentTimeMillis() - 3_600_000,
            unreadCount = 7,
            isDm = false
        )
    )

    // ─── Explore grid images ─────────────────────────────────────
    val exploreImages = listOf(
        Pair("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=70", "@aurora:matrix.org"),
        Pair("https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=400&q=70", "@neon:matrix.org"),
        Pair("https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=400&q=70", "@solaris:matrix.org"),
        Pair("https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&q=70", "@prism:matrix.org"),
        Pair("https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=400&q=70", "@wave:matrix.org"),
        Pair("https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=400&q=70", "@ember:matrix.org"),
        Pair("https://images.unsplash.com/photo-1536098561742-ca998e48cbcc?w=400&q=70", "@neon:matrix.org"),
        Pair("https://images.unsplash.com/photo-1590766740466-39fe019e3fad?w=400&q=70", "@ember:matrix.org"),
        Pair("https://images.unsplash.com/photo-1516655855035-d5215376c7a3?w=400&q=70", "@ember:matrix.org"),
        Pair("https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=400&q=70", "@aurora:matrix.org"),
        Pair("https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=400&q=70", "@solaris:matrix.org"),
        Pair("https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=400&q=70", "@wave:matrix.org"),
        Pair("https://images.unsplash.com/photo-1447752875215-b2761acf3dfd?w=400&q=70", "@aurora:matrix.org"),
        Pair("https://images.unsplash.com/photo-1501854140801-50d01698950b?w=400&q=70", "@wave:matrix.org"),
        Pair("https://images.unsplash.com/photo-1542341502-b4a7d30f5eda?w=400&q=70", "@prism:matrix.org"),
        Pair("https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=400&q=70", "@neon:matrix.org"),
        Pair("https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=400&q=70", "@ember:matrix.org"),
        Pair("https://images.unsplash.com/photo-1455156218388-5e61b526818b?w=400&q=70", "@solaris:matrix.org")
    )
}
