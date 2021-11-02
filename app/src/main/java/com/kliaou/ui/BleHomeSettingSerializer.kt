@file:Suppress("BlockingMethodInNonBlockingContext")

package com.kliaou.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.kliaou.datastore.proto.MyChars
import com.kliaou.datastore.proto.MyNickname
import com.kliaou.datastore.proto.MySex
import com.kliaou.datastore.proto.RemoteSex
import java.io.InputStream
import java.io.OutputStream

object MyCharsSerializer : Serializer<MyChars> {
    override val defaultValue: MyChars
        get() = MyChars.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MyChars {
        return try {
            MyChars.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            exception.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: MyChars, output: OutputStream) {
        t.writeTo(output)
    }
}
object MyNicknameSerializer : Serializer<MyNickname> {
    override val defaultValue: MyNickname
        get() = MyNickname.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MyNickname {
        return try {
            MyNickname.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            exception.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: MyNickname, output: OutputStream) {
        t.writeTo(output)
    }
}
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

val Context.myCharsDataStore: DataStore<MyChars> by dataStore(
    fileName = BleMainSettingViewModel.PREFS_MY_CHARS_FN,
    serializer = MyCharsSerializer
)
val Context.myNicknameDataStore: DataStore<MyNickname> by dataStore(
    fileName = BleMainSettingViewModel.PREFS_MY_NICKNAME_FN,
    serializer = MyNicknameSerializer
)
val Context.mySexDataStore: DataStore<MySex> by dataStore(
    fileName = BleMainSettingViewModel.PREFS_MY_SEX_FN,
    serializer = MySexSerializer
)
val Context.remoteSexDataStore: DataStore<RemoteSex> by dataStore(
    fileName = BleMainSettingViewModel.PREFS_REMOTE_SEX_FN,
    serializer = RemoteSexSerializer
)