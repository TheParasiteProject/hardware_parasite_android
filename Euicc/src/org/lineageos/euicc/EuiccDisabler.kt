/*
 * Copyright (C) 2021-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.euicc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.res.Resources
import android.util.Log

object EuiccDisabler {
    private const val TAG = "EuiccDisabler"

    private fun getStringArrayResSafely(context: Context, resId: Int): Array<String> {
        return try {
            context.resources.getStringArray(resId)
        } catch (e: Resources.NotFoundException) {
            Log.d(TAG, "Failed to get resources.", e)
            emptyArray()
        }
    }

    private fun isInstalled(pm: PackageManager, pkgName: String) =
        runCatching {
                val info = pm.getApplicationInfo(pkgName, ApplicationInfoFlags.of(0))
                info.flags and ApplicationInfo.FLAG_INSTALLED != 0
            }
            .getOrDefault(false)

    private fun isInstalledAndEnabled(pm: PackageManager, pkgName: String) =
        runCatching {
                val info = pm.getApplicationInfo(pkgName, ApplicationInfoFlags.of(0))
                Log.d(TAG, "package $pkgName installed, enabled = ${info.enabled}")
                info.enabled
            }
            .getOrDefault(false)

    fun enableOrDisableEuicc(context: Context) {
        val pm = context.packageManager
        val disable =
            getStringArrayResSafely(context, R.array.config_euicc_depedencies).any {
                !isInstalledAndEnabled(pm, it)
            }
        val flag =
            if (disable) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

        for (pkg in getStringArrayResSafely(context, R.array.config_euicc_packages)) {
            if (isInstalled(pm, pkg)) {
                pm.setApplicationEnabledSetting(pkg, flag, 0)
            }
        }
    }
}
