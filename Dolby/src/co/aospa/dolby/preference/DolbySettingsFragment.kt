/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.preference

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.os.postDelayed
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import co.aospa.dolby.DolbyConstants.Companion.PREF_BASS
import co.aospa.dolby.DolbyConstants.Companion.PREF_DIALOGUE
import co.aospa.dolby.DolbyConstants.Companion.PREF_DIALOGUE_AMOUNT
import co.aospa.dolby.DolbyConstants.Companion.PREF_ENABLE
import co.aospa.dolby.DolbyConstants.Companion.PREF_HP_VIRTUALIZER
import co.aospa.dolby.DolbyConstants.Companion.PREF_IEQ
import co.aospa.dolby.DolbyConstants.Companion.PREF_PRESET
import co.aospa.dolby.DolbyConstants.Companion.PREF_PROFILE
import co.aospa.dolby.DolbyConstants.Companion.PREF_SPK_VIRTUALIZER
import co.aospa.dolby.DolbyConstants.Companion.PREF_STEREO_WIDENING
import co.aospa.dolby.DolbyConstants.Companion.PREF_VOLUME
import co.aospa.dolby.DolbyConstants.Companion.dlog
import co.aospa.dolby.DolbyController
import co.aospa.dolby.R
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference

class DolbySettingsFragment :
    SettingsBasePreferenceFragment(), OnPreferenceChangeListener, OnCheckedChangeListener {

    private val appContext: Context
        get() = requireContext().applicationContext

    private val switchBar by lazy { findPreference<MainSwitchPreference>(PREF_ENABLE)!! }
    private val profilePref by lazy { findPreference<ListPreference>(PREF_PROFILE)!! }
    private val presetPref by lazy { findPreference<Preference>(PREF_PRESET)!! }
    private val ieqPref by lazy { findPreference<DolbyIeqPreference>(PREF_IEQ)!! }
    private val dialoguePref by lazy { findPreference<SwitchPreferenceCompat>(PREF_DIALOGUE)!! }
    private val dialogueAmountPref by lazy {
        findPreference<SliderPreference>(PREF_DIALOGUE_AMOUNT)!!
    }
    private val bassPref by lazy { findPreference<SwitchPreferenceCompat>(PREF_BASS)!! }
    private val hpVirtPref by lazy { findPreference<SwitchPreferenceCompat>(PREF_HP_VIRTUALIZER)!! }
    private val spkVirtPref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_SPK_VIRTUALIZER)!!
    }
    private val settingsCategory by lazy {
        findPreference<PreferenceCategory>("dolby_category_settings")!!
    }
    private val advSettingsCategory by lazy {
        findPreference<PreferenceCategory>("dolby_category_adv_settings")!!
    }
    private val advSettingsFooter by lazy {
        findPreference<Preference>("dolby_adv_settings_footer")!!
    }
    private var volumePref: SwitchPreferenceCompat? = null
    private var stereoPref: SliderPreference? = null

    private val dolbyController by lazy { DolbyController.getInstance(appContext) }
    private val audioManager by lazy { appContext.getSystemService(AudioManager::class.java)!! }
    private val handler = Handler(Looper.getMainLooper())

    private var isOnSpeaker = true
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "setIsOnSpeaker($value)")
            updateProfileSpecificPrefs()
        }

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                dlog(TAG, "onAudioDevicesAdded")
                updateSpeakerState()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                dlog(TAG, "onAudioDevicesRemoved")
                updateSpeakerState()
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        dlog(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.dolby_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dsOn = dolbyController.dsOn
        switchBar.addOnSwitchChangeListener(this)
        switchBar.isChecked = dsOn

        stereoPref = findPreference<SliderPreference>(PREF_STEREO_WIDENING)!!
        if (
            !updatePrefs(
                R.bool.dolby_stereo_widening_supported,
                advSettingsCategory,
                stereoPref,
                true,
            )
        ) {
            stereoPref = null
        }

        volumePref = findPreference<SwitchPreferenceCompat>(PREF_VOLUME)!!
        if (
            !updatePrefs(
                R.bool.dolby_volume_leveler_supported,
                advSettingsCategory,
                volumePref,
                true,
            )
        ) {
            volumePref = null
        }

        preferenceManager.preferenceDataStore =
            DolbyPreferenceStore(appContext).also { it.profile = dolbyController.profile }

        if (updatePrefs(R.bool.dolby_profile_supported, null, profilePref, true)) {
            updateProfileIcon(dolbyController.profile)
        }
        updatePrefs(R.bool.dolby_preset_supported, settingsCategory, presetPref, true)
        updatePrefs(R.bool.dolby_bass_enhancer_supported, settingsCategory, bassPref, true)
        updatePrefs(
            R.bool.dolby_headphone_virtualizer_supported,
            advSettingsCategory,
            hpVirtPref,
            true,
        )
        updatePrefs(
            R.bool.dolby_speaker_virtualizer_supported,
            advSettingsCategory,
            spkVirtPref,
            true,
        )
        updatePrefs(
            R.bool.dolby_dialogue_enhancer_supported,
            advSettingsCategory,
            dialoguePref,
            true,
        )
        if (
            updatePrefs(
                R.bool.dolby_dialogue_enhancer_supported,
                advSettingsCategory,
                dialogueAmountPref,
                true,
            )
        ) {
            dialogueAmountPref.apply {
                onPreferenceChangeListener = this@DolbySettingsFragment
                min = resources.getInteger(R.integer.dialogue_enhancer_min)
                max = resources.getInteger(R.integer.dialogue_enhancer_max)
                sliderIncrement = 1
                setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS)
                setUpdatesContinuously(true)
            }
        }
        updatePrefs(
            R.bool.dolby_intelligent_equalizer_supported,
            advSettingsCategory,
            ieqPref,
            true,
        )

        stereoPref?.apply {
            onPreferenceChangeListener = this@DolbySettingsFragment
            min = resources.getInteger(R.integer.stereo_widening_min)
            max = resources.getInteger(R.integer.stereo_widening_max)
            sliderIncrement = 1
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS)
            setUpdatesContinuously(true)
        }

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        updateSpeakerState()
        updateProfileSpecificPrefsImmediate()
    }

    fun updatePrefs(
        res: Int,
        categ: PreferenceCategory?,
        pref: Preference?,
        setListener: Boolean,
    ): Boolean {
        val supported =
            try {
                resources.getBoolean(res)
            } catch (e: NotFoundException) {
                dlog(TAG, "Failed to get boolean state of ${res}")
                false
            }

        if (!supported) {
            if (categ == null && pref != null) {
                preferenceScreen?.removePreference(pref)
                return supported
            }
            if (pref != null) {
                categ?.removePreference(pref)
            }
        } else {
            if (setListener) {
                pref?.onPreferenceChangeListener = this
            }
        }

        return supported
    }

    override fun onDestroyView() {
        dlog(TAG, "onDestroyView")
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        updateProfileSpecificPrefsImmediate()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        dlog(TAG, "onPreferenceChange: key=${preference.key} value=$newValue")
        when (preference.key) {
            PREF_PROFILE -> {
                val profile = newValue.toString().toInt()
                dolbyController.profile = profile
                updateProfileIcon(profile)
                updateProfileSpecificPrefs()
            }

            PREF_SPK_VIRTUALIZER -> {
                dolbyController.setSpeakerVirtEnabled(newValue as Boolean)
            }

            PREF_HP_VIRTUALIZER -> {
                dolbyController.setHeadphoneVirtEnabled(newValue as Boolean)
            }

            PREF_STEREO_WIDENING -> {
                dolbyController.setStereoWideningAmount(newValue as Int)
            }

            PREF_DIALOGUE -> {
                dolbyController.setDialogueEnhancerEnabled(newValue as Boolean)
            }

            PREF_DIALOGUE_AMOUNT -> {
                dolbyController.setDialogueEnhancerAmount(newValue as Int)
            }

            PREF_BASS -> {
                dolbyController.setBassEnhancerEnabled(newValue as Boolean)
            }

            PREF_VOLUME -> {
                dolbyController.setVolumeLevelerEnabled(newValue as Boolean)
            }

            PREF_IEQ -> {
                dolbyController.setIeqPreset(newValue.toString().toInt())
            }

            else -> return false
        }
        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        dlog(TAG, "onCheckedChanged($isChecked)")
        dolbyController.dsOn = isChecked
        updateProfileSpecificPrefs()
    }

    private fun updateSpeakerState() {
        val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA)[0]
        isOnSpeaker = (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
    }

    private fun updateProfileSpecificPrefs() {
        handler.postDelayed(100) { updateProfileSpecificPrefsImmediate() }
    }

    private fun updateProfileSpecificPrefsImmediate() {
        if (!dolbyController.dsOn) {
            dlog(TAG, "updateProfileSpecificPrefs: Dolby is off")
            advSettingsCategory.isVisible = false
            return
        }

        val unknownRes = getString(R.string.dolby_unknown)
        val headphoneRes = getString(R.string.dolby_connect_headphones)
        val currentProfile = dolbyController.profile
        val isDynamicProfile = currentProfile == 0
        (preferenceManager.preferenceDataStore as DolbyPreferenceStore).profile = currentProfile

        dlog(TAG, "updateProfileSpecificPrefs: currentProfile=$currentProfile")

        profilePref.apply {
            if (entryValues.contains(currentProfile.toString())) {
                summary = "%s"
                value = currentProfile.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "current profile $currentProfile unknown")
            }
        }

        // hide advanced settings on dynamic profile
        advSettingsCategory.isVisible = !isDynamicProfile
        advSettingsFooter.isVisible = isDynamicProfile

        presetPref.summary = dolbyController.getPresetName()
        bassPref.isChecked = dolbyController.getBassEnhancerEnabled(currentProfile)

        // below prefs are not visible on dynamic profile
        if (isDynamicProfile) return

        val ieqValue = dolbyController.getIeqPreset(currentProfile)
        ieqPref.apply {
            if (entryValues.contains(ieqValue.toString())) {
                summary = "%s"
                value = ieqValue.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "ieq value $ieqValue unknown")
            }
        }

        dialoguePref.isChecked = dolbyController.getDialogueEnhancerEnabled(currentProfile)
        dialogueAmountPref.value = dolbyController.getDialogueEnhancerAmount(currentProfile)
        spkVirtPref.isChecked = dolbyController.getSpeakerVirtEnabled(currentProfile)
        volumePref?.isChecked = dolbyController.getVolumeLevelerEnabled(currentProfile)
        hpVirtPref.isChecked = dolbyController.getHeadphoneVirtEnabled(currentProfile)
        stereoPref?.value = dolbyController.getStereoWideningAmount(currentProfile)
    }

    private fun updateProfileIcon(profile: Int) {
        val profiles = resources.getStringArray(R.array.dolby_profile_values)
        val icons = resources.obtainTypedArray(R.array.dolby_profile_icons)
        try {
            val index = profiles.indexOf(profile.toString())

            if (index != -1 && index < icons.length()) {
                profilePref.setIcon(icons.getResourceId(index, 0))
            } else {
                profilePref.setIcon(R.drawable.ic_dolby)
            }
        } finally {
            icons.recycle()
        }
    }

    companion object {
        private const val TAG = "DolbySettingsFragment"
        private val ATTRIBUTES_MEDIA =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
    }
}
