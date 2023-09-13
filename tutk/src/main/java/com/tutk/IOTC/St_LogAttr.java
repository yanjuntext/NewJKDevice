package com.tutk.IOTC;

import androidx.annotation.Keep;

@Keep
public class St_LogAttr {
    public LogLevel log_level = LogLevel.LEVEL_VERBOSE;
    public String path = "";
    public int file_max_size = 0;
    public int file_max_count = 0;
}
