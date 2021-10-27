package com.kliaou.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EntitySetting(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?,
    @ColumnInfo(name = "nick_name") val nickName: String?,
    @ColumnInfo(name = "my_gender") val myGender: Int?,
    @ColumnInfo(name = "remote_gender") val remoteGender: Int?,
)