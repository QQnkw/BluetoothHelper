package com.bluetooth.bluetoothhelper

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import java.util.*
import kotlin.experimental.and

fun Fragment.toast(content: String) {
    Toast.makeText(requireContext(), content, Toast.LENGTH_SHORT).show()
}

fun Any.toJson(): String {
    return try {
        Gson().toJson(this)
    } catch (exception: Exception) {
        ""
    }
}

fun <T> String.jsonToBean(clazz: Class<T>): T? {
    return try {
        Gson().fromJson(this, clazz)
    } catch (exception: Exception) {
        null
    }
}

fun String.toUUID(): UUID {
    return UUID.fromString("0000$this-0000-1000-8000-00805f9b34fb")
}

fun ByteArray.toHexString(): String {
    return if (size > 0) {
        val stringBuilder = StringBuilder(size)
        for (byteChar in this) stringBuilder.append(
            String.format(
                "%02X",
                byteChar
            )
        )
        stringBuilder.toString()
    } else {
        ""
    }
}

fun String.hexToAscii(): String {
    val output = StringBuilder("")
    var i = 0
    while (i < length) {
        val str = substring(i, i + 2)
        output.append(str.toInt(16).toChar())
        i += 2
    }
    return output.toString()
}

fun String.hex2Decimal(): Int {
    var value = this
    val digits = "0123456789ABCDEF"
    value = value.uppercase(Locale.ENGLISH)
    var result = 0
    for (element in value) {
        val d = digits.indexOf(element)
        result = 16 * result + d
    }
    return result
}

fun String.asciiToHexString(): String {
    var asciiStr = this
    if (asciiStr.length > 14) {
        asciiStr = asciiStr.substring(0, 14)
    }
    val chars = asciiStr.toCharArray()
    val hex = StringBuilder()
    val buildBytes = ByteArray(15)
    buildBytes[0] = 0xff.toByte()
    var counter = 1
    for (ch in chars) {
        hex.append(Integer.toHexString(ch.code))
        counter++
    }
    return hex.toString()
}

fun String.hexStringToByteArray(): ByteArray {
    var bytesString = this.removePrefix("0x").filter { it != ':' }
    if (bytesString.length % 2 == 1) {
        bytesString = "0$bytesString"
    }
    val result = ByteArray(bytesString.length / 2)
    for (i in bytesString.indices step 2) {
        val resultIndex = i.shr(1)
        val byteValue = bytesString.substring(i, i + 2).hexStringToByte()
        result[resultIndex] = byteValue
    }

    return result
}

fun String.hexStringToByte(): Byte {
    val hexChars = "0123456789ABCDEF"
    if (length < 2) {
        return Byte.MIN_VALUE
    }

    val thisUppercase = uppercase(Locale.ENGLISH)

    val firstIndex = hexChars.indexOf(thisUppercase[0])
    val secondIndex = hexChars.indexOf(thisUppercase[1])

    if (firstIndex == -1 || secondIndex == -1) {
        return Byte.MIN_VALUE
    }

    val octet = firstIndex.shl(4).or(secondIndex)
    return octet.toByte()
}

fun String.macToChipID(): String {
    var tempMac = this
    var chipID = ""
    tempMac = tempMac.replace(":", "")
    val macAddr: ByteArray = tempMac.hexStringToByteArray()
    val systemId = ByteArray(8)
    systemId[0] = macAddr[5]
    systemId[1] = macAddr[4]
    systemId[2] = macAddr[3]
    systemId[3] = 0
    systemId[4] = 0
    systemId[5] = macAddr[2]
    systemId[6] = macAddr[1]
    systemId[7] = macAddr[0]
    val tmpChipIDString: String = systemId.toHexString()
    val tmpChipID: ByteArray = tmpChipIDString.hexStringToByteArray()
    tmpChipID[0] = ((systemId[2] and 0x0f) + (systemId[7] and 0xf0.toByte())).toByte()
    tmpChipID[1] = ((systemId[5] and 0x0f) + (systemId[1] and 0xf0.toByte())).toByte()
    tmpChipID[2] = ((systemId[7] and 0x0f) + (0xf0 - (systemId[6] and 0xf0.toByte()))).toByte()
    tmpChipID[3] = (0xff - systemId[6]).toByte()
    tmpChipID[4] = (systemId[5] + 1).toByte()
    tmpChipID[5] = ((systemId[1] and 0x0f) + (0xf0 - (systemId[2] and 0xf0.toByte()))).toByte()
    tmpChipID[6] = ((systemId[0] and 0x0f) + (systemId[5] and 0xf0.toByte())).toByte()
    tmpChipID[7] = ((systemId[6] and 0x0f) + (systemId[0] and 0xf0.toByte())).toByte()
    chipID = tmpChipID.toHexString()
    return chipID
}
