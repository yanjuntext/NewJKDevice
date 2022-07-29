package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public enum LogLevel {
    LEVEL_VERBOSE(0),
    LEVEL_DEBUG(1),
    LEVEL_INFO(2),
    LEVEL_WARNING(3),
    LEVEL_ERROR(4),
    LEVEL_SILENCE(5);

    private int value;

    private LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
