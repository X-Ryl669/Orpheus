/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.library.kutr.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.method.PasswordTransformationMethod;
import android.widget.FrameLayout;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.kutr.R;

import java.util.List;

/**
 * Created by X-Ryl669.
 */
public class LoginActivity extends AppCompatPreferenceActivity {

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Integer passwordType = android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD | android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof EditTextPreference
                    && (((EditTextPreference)preference).getEditText().getInputType() & passwordType) > 0) {
                preference.setSummary(stringValue);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean light = getIntent().getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, true);
        setTheme(light ? R.style.KutrLightTheme : R.style.KutrDarkTheme);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setupActionBar();

/*
        FrameLayout main = new FrameLayout(this);
        main.setId(R.id.list);
        setContentView(main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.list, new LoginFragment(), "login_fragment")
                    .commit();
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.login_header, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || LoginFragment.class.getName().equals(fragmentName);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LoginFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.login);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("host"));
            bindPreferenceSummaryToValue(findPreference("account"));

            // Remove the password from the displayed list of preference, since the password is
            // computed from clear text
            PreferenceScreen screen = getPreferenceScreen();
            Preference pref = getPreferenceManager().findPreference("password");
            screen.removePreference(pref);

            // And add a preference to the list to let the user enter the "clear text" password
            EditTextPreference password = new EditTextPreference(pref.getContext());
            password.setKey("passclear");
            password.setTitle("Password on the server");
            password.getEditText().setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.getEditText().setTransformationMethod(new PasswordTransformationMethod());
            password.setDialogTitle("Password");
            password.setSummary(getPreferenceManager().getSharedPreferences().getString("password", "").length() > 0 ? "***" : "None");
            // Update the summary dynamically so it either display "None" or a fake password
            password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString().length() > 0 ? "***" : "None");
                    return true;
                }
            });
            screen.addPreference(password);
/*
            Preference host = findPreference("host");
            host.setSummary("http://yourshere");
            Preference account = findPreference("account");
            account.setSummary("me");
            Preference password = findPreference("password");
            password.setSummary("****");
            */
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CacheFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.login);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("host"));
            bindPreferenceSummaryToValue(findPreference("account"));

            // Remove the password from the displayed list of preference, since the password is
            // computed from clear text
            PreferenceScreen screen = getPreferenceScreen();
            Preference pref = getPreferenceManager().findPreference("password");
            screen.removePreference(pref);

            // And add a preference to the list to let the user enter the "clear text" password
            EditTextPreference password = new EditTextPreference(pref.getContext());
            password.setKey("passclear");
            password.setTitle("Password on the server");
            password.getEditText().setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.getEditText().setTransformationMethod(new PasswordTransformationMethod());
            password.setDialogTitle("Password");
            password.setSummary(getPreferenceManager().getSharedPreferences().getString("password", "").length() > 0 ? "***" : "None");
            // Update the summary dynamically so it either display "None" or a fake password
            password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString().length() > 0 ? "***" : "None");
                    return true;
                }
            });
            screen.addPreference(password);
/*
            Preference host = findPreference("host");
            host.setSummary("http://yourshere");
            Preference account = findPreference("account");
            account.setSummary("me");
            Preference password = findPreference("password");
            password.setSummary("****");
            */
        }
    }
}
