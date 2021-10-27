package com.kliaou.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    EntitySetting::class,
    EntityDevice::class
                     ], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun daoSetting(): DaoSetting
    abstract fun daoDevice(): DaoDevice
}