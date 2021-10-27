package com.kliaou.db

import androidx.room.*
import com.google.protobuf.LazyStringArrayList

@Dao
interface DaoDevice {
    @Query("SELECT * FROM EntityDevice")
    fun getAll(): List<EntityDevice>

    @Query("SELECT * FROM EntityDevice WHERE deviceName IN (:names)")
    fun loadAllByNames(names: List<String>): List<EntityDevice>

    @Query("SELECT * FROM EntityDevice WHERE deviceName LIKE :name LIMIT 1")
    fun findByName(name: String): EntityDevice

    @Insert
    fun insertAll(vararg devices: EntityDevice)

    @Update
    fun update(vararg devices: EntityDevice)

    @Delete
    fun delete(device: EntityDevice)
}