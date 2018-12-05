/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Observer
import com.duckduckgo.app.about.AboutDuckDuckGoActivity
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.feedback.ui.FeedbackActivity
import com.duckduckgo.app.global.DuckDuckGoActivity
import com.duckduckgo.app.global.sendThemeChangedBroadcast
import com.duckduckgo.app.global.view.launchDefaultAppActivity
import com.duckduckgo.app.onboarding.ui.OnboardingActivity
import com.duckduckgo.app.settings.clear.ClearWhatOption
import com.duckduckgo.app.settings.clear.ClearWhenOption
import com.duckduckgo.app.settings.SettingsViewModel.AutomaticallyClearData
import com.duckduckgo.app.settings.SettingsViewModel.Command
import kotlinx.android.synthetic.main.content_settings_general.*
import kotlinx.android.synthetic.main.content_settings_other.*
import kotlinx.android.synthetic.main.content_settings_privacy.*
import kotlinx.android.synthetic.main.include_toolbar.*

class SettingsActivity : DuckDuckGoActivity(), SettingsAutomaticallyClearWhatFragment.Listener, SettingsAutomaticallyClearWhenFragment.Listener {

    private val viewModel: SettingsViewModel by bindViewModel()

    private val defaultBrowserChangeListener = OnCheckedChangeListener { _, _ -> launchDefaultAppScreen() }

    private val lightThemeToggleListener = OnCheckedChangeListener { _, isChecked ->
        viewModel.onLightThemeToggled(isChecked)
    }

    private val autocompleteToggleListener = OnCheckedChangeListener { _, isChecked ->
        viewModel.onAutocompleteSettingChanged(isChecked)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupActionBar()

        configureUiEventHandlers()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
    }

    private fun configureUiEventHandlers() {
        onboarding.setOnClickListener { startActivity(OnboardingActivity.intent(this)) }
        about.setOnClickListener { startActivity(AboutDuckDuckGoActivity.intent(this)) }
        provideFeedback.setOnClickListener { viewModel.userRequestedToSendFeedback() }

        lightThemeToggle.setOnCheckedChangeListener(lightThemeToggleListener)
        autocompleteToggle.setOnCheckedChangeListener(autocompleteToggleListener)
        setAsDefaultBrowserSetting.setOnCheckedChangeListener(defaultBrowserChangeListener)
        automaticallyClearWhatSetting.setOnClickListener { launchAutomaticallyClearWhatDialog() }
        automaticallyClearWhenSetting.setOnClickListener { launchAutomaticallyClearWhenDialog() }
    }

    private fun observeViewModel() {
        viewModel.viewState.observe(this, Observer<SettingsViewModel.ViewState> { viewState ->
            viewState?.let {
                version.setSubtitle(it.version)
                lightThemeToggle.quietlySetIsChecked(it.lightThemeEnabled, lightThemeToggleListener)
                autocompleteToggle.quietlySetIsChecked(it.autoCompleteSuggestionsEnabled, autocompleteToggleListener)
                updateDefaultBrowserViewVisibility(it)
                updateAutomaticClearDataOptions(it.automaticallyClearData)
            }
        })

        viewModel.command.observe(this, Observer {
            processCommand(it)
        })
    }

    private fun updateAutomaticClearDataOptions(automaticallyClearData: AutomaticallyClearData) {
        val clearWhatSubtitle = getString(automaticallyClearData.clearWhatOption.nameStringRes)
        automaticallyClearWhatSetting.setSubtitle(clearWhatSubtitle)

        val clearWhenSubtitle = getString(automaticallyClearData.clearWhenOption.nameStringRes)
        automaticallyClearWhenSetting.setSubtitle(clearWhenSubtitle)

        val whenOptionEnabled = automaticallyClearData.clearWhenOptionEnabled
        automaticallyClearWhenSetting.isEnabled = whenOptionEnabled
    }

    private fun launchAutomaticallyClearWhatDialog() {
        val dialog = SettingsAutomaticallyClearWhatFragment.create(viewModel.viewState.value?.automaticallyClearData?.clearWhatOption)
        dialog.show(supportFragmentManager, CLEAR_WHAT_DIALOG_TAG)
    }

    private fun launchAutomaticallyClearWhenDialog() {
        val dialog = SettingsAutomaticallyClearWhenFragment.create(viewModel.viewState.value?.automaticallyClearData?.clearWhenOption)
        dialog.show(supportFragmentManager, CLEAR_WHEN_DIALOG_TAG)
    }

    private fun processCommand(it: Command?) {
        when (it) {
            is Command.LaunchFeedback -> launchFeedback()
            is Command.UpdateTheme -> sendThemeChangedBroadcast()
        }
    }

    private fun updateDefaultBrowserViewVisibility(it: SettingsViewModel.ViewState) {
        if (it.showDefaultBrowserSetting) {
            setAsDefaultBrowserSetting.quietlySetIsChecked(it.isAppDefaultBrowser, defaultBrowserChangeListener)
            setAsDefaultBrowserSetting.visibility = View.VISIBLE
        } else {
            setAsDefaultBrowserSetting.visibility = View.GONE
        }
    }

    private fun launchDefaultAppScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            launchDefaultAppActivity()
        } else {
            throw IllegalStateException("Unable to launch default app activity on this OS")
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun launchFeedback() {
        startActivity(Intent(FeedbackActivity.intent(this)))
    }

    override fun onAutomaticallyClearWhatOptionSelected(clearWhatSetting: ClearWhatOption) {
        viewModel.onAutomaticallyWhatOptionSelected(clearWhatSetting)
    }

    override fun onAutomaticallyClearWhenOptionSelected(clearWhenSetting: ClearWhenOption) {
        viewModel.onAutomaticallyWhenOptionSelected(clearWhenSetting)
    }

    companion object {
        private const val CLEAR_WHAT_DIALOG_TAG = "CLEAR_WHAT_DIALOG_FRAGMENT"
        private const val CLEAR_WHEN_DIALOG_TAG = "CLEAR_WHEN_DIALOG_FRAGMENT"

        fun intent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}

/**
 * Utility method to toggle a switch without broadcasting to its change listener
 *
 * This is useful for when setting the checked state from the view model where we want the switch state to match some value, but this act itself
 * should not result in the checked change event handler being fired
 *
 * Requires the change listener to be provided explicitly as it is held privately in the super class and cannot be accessed automatically.
 */
private fun SwitchCompat.quietlySetIsChecked(newCheckedState: Boolean, changeListener: OnCheckedChangeListener?) {
    setOnCheckedChangeListener(null)
    isChecked = newCheckedState
    setOnCheckedChangeListener(changeListener)
}
