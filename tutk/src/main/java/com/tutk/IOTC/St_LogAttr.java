package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class St_LogAttr {
    public LogLevel log_level = LogLevel.LEVEL_VERBOSE;
    public String path = "";
    public int file_max_size = 0;
    public int file_max_count = 0;
}
