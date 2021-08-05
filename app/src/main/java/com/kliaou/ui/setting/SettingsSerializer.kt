package com.kliaou.ui.setting

import android.content.Context
import android.provider.ContactsContract
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.kliaou.datastore.proto.MySex
import com.kliaou.datastore.proto.RemoteSex
import java.io.InputStream
import java.io.OutputStream

object MySexSerializer : Serializer<MySex> {
    override val defaultValue: MySex
        get() = MySex.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MySex {
        return try {
            MySex.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            exception.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: MySex, output: OutputStream) {
        t.writeTo(output)
    }
}

object RemoteSexSerializer : Serializer<RemoteSex> {
    override val defaultValue: RemoteSex
        get() = RemoteSex.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): RemoteSex {
        return try {
            RemoteSex.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            exception.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: RemoteSex, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.mySexDataStore: DataStore<MySex> by dataStore(
    fileName = SettingViewModel.PREFS_MY_SEX_FN,
    serializer = MySexSerializer
)
val Context.remoteSexDataStore: DataStore<RemoteSex> by dataStore(
    fileName = SettingViewModel.PREFS_REMOTE_SEX_FN,
    serializer = RemoteSexSerializer
)