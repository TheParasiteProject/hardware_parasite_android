/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Display.HdrCapabilities
import co.aospa.dolby.DolbyConstants.Companion.dlog

internal class DolbyVisionController private constructor(private val context: Context) {
    init {
        dlog(TAG, "initialized")
    }

    fun onBootCompleted() {
        dlog(TAG, "onBootCompleted")

        if (!context.resources.getBoolean(R.bool.dolby_vision_overrides_hdr_types)) {
            return
        }

        // Override HDR types to enable Dolby Vision
        overrideHdrTypes()
    }

    private fun overrideHdrTypes() {
        val dm: DisplayManager = context.getSystemService(DisplayManager::class.java) ?: return
        dm.overrideHdrTypes(
            Display.DEFAULT_DISPLAY,
            intArrayOf(
                HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG,
                HdrCapabilities.HDR_TYPE_HDR10_PLUS,
            ),
        )
    }

    companion object {
        private const val TAG = "DolbyVisionController"

        @Volatile private var instance: DolbyVisionController? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: DolbyVisionController(context).also { instance = it }
                }
    }
}
