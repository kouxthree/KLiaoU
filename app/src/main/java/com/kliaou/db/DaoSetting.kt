package com.kliaou.db

import androidx.room.*

@Dao
interface DaoSetting {

    @Query("SELECT * FROM EntitySetting")
    fun getAll(): List<EntitySetting>?

    @Query("SELECT * FROM EntitySetting LIMIT 1")
    fun getFirst(): EntitySetting?

    @Query("SELECT * FROM EntitySetting WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<EntitySetting>?

    @Query("SELECT * FROM EntitySetting WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): EntitySetting?

    @Insert
    fun insertAll(vararg users: EntitySetting)

    @Update
    fun update(vararg users: EntitySetting)

    @Delete
    fun delete(user: EntitySetting)

}