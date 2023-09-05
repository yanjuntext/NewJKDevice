package com.tutk.IOTC

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
object Packet {

    //小端  数组转short
    fun byteArrayToShort_Little(data: ByteArray, start: Int): Short {
        return ((0xff and (data[start].toInt())) or ((0xff and data[start + 1].toInt()) shl 8)).toShort()
    }

    //小端  数组转int
    fun byteArrayToInt_Little(data: ByteArray, start: Int = 0): Int {
        val total = data.size
        return when {
            total > start + 3 -> {
                (data[start].toInt() and 0xff) or ((data[start + 1].toInt() and 0xff) shl 8) or ((data[start + 2].toInt() and 0xff) shl 16) or ((data[start + 3].toInt() and 0xff) shl 24)
            }
            total > start + 1 -> {
                (data[start].toInt() and 0xff) or ((data[start + 1].toInt() and 0xff) shl 8)
            }
            total > start -> {
                (data[start].toInt() and 0xff)
            }
            else -> 0
        }
    }

    //小端  数组转long
    fun byteArrayToLong_Little(data: ByteArray, start: Int = 0): Long {
        return ((data[start].toInt() and 0xff) or ((data[start + 1].toInt() and 0xff) shl 8) or ((data[start + 2].toInt() and 0xff) shl 16) or ((data[start + 3].toInt() and 0xff) shl 24) or
                ((data[start + 1].toInt() and 0xff) shl 32) or ((data[start + 1].toInt() and 0xff) shl 40) or ((data[start + 1].toInt() and 0xff) shl 48) or ((data[start + 1].toInt() and 0xff) shl 56)).toLong()
    }

    fun byteArrayToLong(data: ByteArray,big:Boolean,start:Int):Long{
        if(data.size < start + 8) return 0L
        var value = 0L
        if(big){
            (start until (start+8)).forEach { index->
                val it = data[index].toLong()
                value = value shl 8
                value = value or (it and 0x00000000000000ff)
            }
        }else{
            val count = start + 7
            (count downTo start).forEach{index->
                val b = data[index].toLong()
                value = value shl 8
                value = value or (b and 0x00000000000000ff)
            }

        }
        return value
    }

    //大端 数组转int
    fun byteArrayToInt_big(data: ByteArray, start: Int = 0): Int {
        val total = data.size
        return when {
            total > start + 3 -> {
                ((data[start].toInt() and 0xff) shl 24) or ((data[start + 1].toInt() and 0xff) shl 16) or ((data[start + 2].toInt() and 0xff) shl 8) or ((data[start + 3].toInt() and 0xff))
            }
            total > start + 1 -> {
                ((data[start].toInt() and 0xff) shl 8) or ((data[start + 1].toInt() and 0xff))
            }
            total > start -> {
                (data[start].toInt() and 0xff)
            }
            else -> 0
        }
    }


    fun longToByteArray_Little(value: Long): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(), (value ushr 24).toByte(), (value ushr 32).toByte(),
            (value ushr 40).toByte(), (value ushr 48).toByte(), (value ushr 56).toByte()
        )
    }

    fun longToByteArray_Big(value: Long): ByteArray {
        return byteArrayOf(
            (value ushr 56).toByte(), (value ushr 48).toByte(), (value ushr 40).toByte(),
            (value ushr 32).toByte(), (value ushr 24).toByte(), (value ushr 16).toByte(),
            (value ushr 8).toByte(), value.toByte()
        )
    }

    fun intToByteArray_Little(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte()
        )
    }

    fun intToByteArray_Big(value: Int): ByteArray {
        return byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte()
        )
    }

    fun shortToByteArray_little(value: Short): ByteArray {
        return byteArrayOf(value.toByte(), (value.toInt() ushr 8).toByte())
    }

    fun shortToByteArray_Big(value: Short): ByteArray {
        return byteArrayOf((value.toInt() ushr 8).toByte(), value.toByte())
    }


    fun intToShort(value: Int): ShortArray {
        return shortArrayOf((value and 0x0000ffff).toShort(), (value shr 16).toShort())
    }

}

fun Int.littleByteArray() = Packet.intToByteArray_Little(this)
fun Int.bigByteArray() = Packet.intToByteArray_Big(this)

fun Short.littleByteArray() = Packet.shortToByteArray_little(this)
fun Short.bigByteArray() = Packet.shortToByteArray_Big(this)

fun Long.littleByteArray() = Packet.longToByteArray_Little(this)
fun Long.bigByteArray() = Packet.longToByteArray_Big(this)

fun ByteArray.littleInt(index: Int) = Packet.byteArrayToInt_Little(this, index)
fun ByteArray.bigInt(index: Int) = Packet.byteArrayToInt_big(this, index)

fun ByteArray.littleShort(index: Int) = Packet.byteArrayToShort_Little(this, index)
fun ByteArray.littleLong(index: Int) = Packet.byteArrayToLong_Little(this, index)

fun Int.shortArray() = Packet.intToShort(this)

