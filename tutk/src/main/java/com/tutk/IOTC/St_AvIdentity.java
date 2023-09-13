/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AvIdentity                                                    *
 *                                                                            *
 * Author: C.T                                                              *
 *                                                                            *
 * Date: 2018/07/03                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

import androidx.annotation.Keep;

//base on the struct AvIdentity in AVAPIs.h
@Keep
public class St_AvIdentity
{
    public int length;
    public String identity = null;
}