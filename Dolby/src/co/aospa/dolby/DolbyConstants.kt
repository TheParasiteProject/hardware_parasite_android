/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.media.AudioDeviceInfo
import android.util.Log

class DolbyConstants {

    enum class DsParam(val id: Int, val length: Int = 1) {
        HEADPHONE_VIRTUALIZER(101),
        SPEAKER_VIRTUALIZER(102),
        VOLUME_LEVELER_ENABLE(103),
        IEQ_PRESET(104),
        DIALOGUE_ENHANCER_ENABLE(105),
        DIALOGUE_ENHANCER_AMOUNT(108),
        GEQ_BAND_GAINS(110, 20),
        BASS_ENHANCER_ENABLE(111),
        STEREO_WIDENING_AMOUNT(113);

        override fun toString(): String {
            return "${name}(${id})"
        }
    }

    enum class DsTuning(val id: Int) {
        INTERNAL_SPEAKER(0),
        HDMI(1),
        MIRACAST(2),
        HEADPHONE(3),
        BLUETOOTH(4),
        USB(5);

        override fun toString(): String {
            return "${name}"
        }

        companion object {
            fun getActivePort(type: Int): Int {
                return when (type) {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> INTERNAL_SPEAKER.id
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> HEADPHONE.id
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> BLUETOOTH.id
                    AudioDeviceInfo.TYPE_HDMI -> HDMI.id
                    AudioDeviceInfo.TYPE_DOCK,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> USB.id
                    AudioDeviceInfo.TYPE_HDMI_ARC,
                    AudioDeviceInfo.TYPE_HDMI_EARC -> HDMI.id
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> INTERNAL_SPEAKER.id
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_BLE_BROADCAST -> BLUETOOTH.id
                    else -> INTERNAL_SPEAKER.id
                }
            }
        }
    }

    companion object {
        const val TAG = "Dolby"
        const val PREF_ENABLE = "dolby_enable"
        const val PREF_PROFILE = "dolby_profile"
        const val PREF_PRESET = "dolby_preset"
        const val PREF_IEQ = "dolby_ieq"
        const val PREF_HP_VIRTUALIZER = "dolby_virtualizer"
        const val PREF_SPK_VIRTUALIZER = "dolby_spk_virtualizer"
        const val PREF_STEREO_WIDENING = "dolby_stereo_widening"
        const val PREF_DIALOGUE = "dolby_dialogue_enabled"
        const val PREF_DIALOGUE_AMOUNT = "dolby_dialogue_amount"
        const val PREF_BASS = "dolby_bass"
        const val PREF_VOLUME = "dolby_volume"
        const val PREF_RESET = "dolby_reset"

        val PROFILE_SPECIFIC_PREFS =
            setOf(
                PREF_PRESET,
                PREF_IEQ,
                PREF_HP_VIRTUALIZER,
                PREF_SPK_VIRTUALIZER,
                PREF_STEREO_WIDENING,
                PREF_DIALOGUE,
                PREF_DIALOGUE_AMOUNT,
                PREF_BASS,
                PREF_VOLUME,
            )

        fun dlog(tag: String, msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
                Log.d("$TAG-$tag", msg)
            }
        }
    }
}
