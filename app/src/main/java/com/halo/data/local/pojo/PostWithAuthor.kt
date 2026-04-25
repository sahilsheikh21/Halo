package com.halo.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.halo.data.local.entity.PostEntity
import com.halo.data.local.entity.UserEntity

data class PostWithAuthor(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "author_id",
        entityColumn = "user_id"
    )
    val author: UserEntity?
)
