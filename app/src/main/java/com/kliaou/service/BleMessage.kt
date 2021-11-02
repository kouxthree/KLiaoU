package com.kliaou.service

/**
 * This sealed class represents the messages sent between connected devices.
 * The RemoteMessage class represents a message coming from a remote device.
 * The LocalMessage class represents a message the user wants to send to the remote device.
 *
 * @param text is the message text the user sends to the other connected device.
 */
sealed class BleMessage(val text: String) {
    class RemoteMessage(text: String) : BleMessage(text)
    class LocalMessage(text: String) : BleMessage(text)
}