package com.tutk.IOTC;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
public enum Region {
    REGION_ALL(0),
    REGION_CN(1),
    REGION_EU(2),
    REGION_US(3),
    REGION_ASIA(4);

    private int value;

    private Region(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
