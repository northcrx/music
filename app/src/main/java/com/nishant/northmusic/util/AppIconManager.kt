package com.nishant.northmusic.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.nishant.northmusic.data.model.AppIcon

object AppIconManager {

    fun setAppIcon(context: Context, targetIcon: AppIcon) {
        val pm = context.packageManager
        val packageName = context.packageName

        AppIcon.entries.forEach { icon ->
            val component = ComponentName(packageName, "$packageName.${icon.aliasName}")
            val desiredState = if (icon == targetIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            try {
                val currentState = pm.getComponentEnabledSetting(component)
                if (currentState != desiredState) {
                    pm.setComponentEnabledSetting(
                        component,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
