package com.kliaou.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EntityDevice(
    @PrimaryKey val deviceName: String,
    @ColumnInfo(name = "mac") val mac: String?,
    @ColumnInfo(name = "type") val type: Int,//0:I scanned  1:connected to me
    @ColumnInfo(name = "display") val display: Boolean?,//0:display 1:non display
)