/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.api;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 11/11/14.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config( manifest = org.robolectric.annotation.Config.NONE)
public class ConfigTest {

    Config config_1;

    @Before
    public void setUp() {
        config_1 = new Config.Builder().addAbility(Config.SEARCHABLE).addAbility(Config.SETTINGS)
                .setPickerComponent(new ComponentName("com.test", "TestClass"))
                .setSettingsComponent(new ComponentName("com.test", "TestClassSettings"))
                .build();
    }

    @Test
    public void testConfigMaterializes() {
        Bundle b = config_1.dematerialize();
        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b2 = Bundle.CREATOR.createFromParcel(p);
        Config fromB = Config.materialize(b2);
        assertThat(config_1.apiVersion).isEqualTo(fromB.apiVersion);
        assertThat(config_1.capabilities).isEqualTo(fromB.capabilities);
        assertThat(config_1.pickerComponent).isEqualTo(fromB.pickerComponent);
        assertThat(config_1.settingsComponent).isEqualTo(fromB.settingsComponent);
    }

}
