package com.tutk.utils

import java.util.*

/**
 * @Author: wangyj
 * @CreateDate: 2021/12/6
 * @Description:
 */

/**
 * 获取当前时区与0时区的偏移分钟
 */
fun getDiffMinute0Zone():Int{
    val instance = Calendar.getInstance()
    return TimeZone.getDefault().getOffset(instance.timeInMillis)/1000/60
}
