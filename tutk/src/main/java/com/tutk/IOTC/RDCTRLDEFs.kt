package com.tutk.IOTC

/**
 * @Author: wangyj
 * @CreateDate: 2021/10/8
 * @Description:
 */
object RDCTRLDEFs {
    const val RDT_COMMAND_FILE_TOTAL: Byte = 0x00
    const val RDT_COMMAND_FILE_NAME: Byte = 0x01
    const val RDT_COMMAND_FILE_SIZE: Byte = 0x02
    const val RDT_COMMAND_FILE_TIMES: Byte = 0x03
    const val RDT_COMMAND_FILE_START: Byte = 0x04
    const val RDT_COMMAND_FILE_STOP: Byte = 0x05
    const val RDT_COMMAND_START: Byte = 0x06
    const val RDT_COMMAND_STOP: Byte = 0x07
    const val RDT_COMMAND_NEXT_FILE: Byte = 0x08

    fun parseContent(type:Byte,_content:ByteArray?):ByteArray?{
        val data = ByteArray(128)
        if(_content == null){
            return null
        }

        data[0] = type
        System.arraycopy(_content,0,data,1,_content.size)
        return data
    }


}