/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Lem Dulfo,
    vanous

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package blk.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blk.freeyourgadget.gadgetbridge.GBApplication;
import blk.freeyourgadget.gadgetbridge.R;
import blk.freeyourgadget.gadgetbridge.util.Prefs;

public class DiscoveryPairingPreferenceActivity extends AbstractSettingsActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSettingsActivity.class);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.discovery_pairing_preferences);

        final Prefs prefs = GBApplication.getPrefs();
        final Preference pref = findPreference("scanning_intensity");
        pref.setSummary(String.valueOf(prefs.getInt("scanning_intensity", 1)));

        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                preference.setSummary(newVal.toString());
                return true;
            }
        });
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}
