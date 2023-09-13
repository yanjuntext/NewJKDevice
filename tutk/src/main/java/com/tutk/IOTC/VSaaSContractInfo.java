/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: VSaaSContractInfo                                                 *
 *                                                                            *
 * Author: Roger                                                              *
 *                                                                            *
 * Date: 2018/05/28                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

import androidx.annotation.Keep;

//base on the struct VSaaSContractInfo in AVAPIs.h
@Keep
public class VSaaSContractInfo
{
    public int contract_type;
    public int vsaas_type;
    public int event_recording_max_sec;
    public int video_max_fps;
    public int recording_max_kbps;
    public int video_max_high;
    public int video_max_width;
}

