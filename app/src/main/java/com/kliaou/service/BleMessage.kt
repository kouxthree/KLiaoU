package com.kliaou.service

sealed class BleMessage(val text: String) {
    class RemoteMessage(text: String) : BleMessage(text)
    class LocalMessage(text: String) : BleMessage(text)
}