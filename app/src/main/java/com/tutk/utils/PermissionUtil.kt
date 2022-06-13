package com.tutk.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.security.Permissions

/**
 * @Author: wangyj
 * @CreateDate: 2021/10/15
 * @Description:
 */
internal object PermissionUtil {

    /**
     *
     * 方法的返回结果
     *  PackageManager.PERMISSION_GRANTED//授予权限
     *  PackageManager.PERMISSION_DENIED//拒绝权限
     *
     * */
    fun permissionIsGranted(context: Context?,vararg permissions: String):Boolean{
        if(context == null) return false
        return run outside@{
            permissions.forEach {
                if(ContextCompat.checkSelfPermission(context,it) != PackageManager.PERMISSION_GRANTED){
                    return@outside false
                }
            }
            return true
        }
    }

}