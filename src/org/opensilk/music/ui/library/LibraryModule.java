/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.library;

import com.squareup.otto.Bus;

import org.opensilk.music.AppModule;
import org.opensilk.music.ui.cards.CardModule;
import org.opensilk.common.dagger.qualifier.ForFragment;
import org.opensilk.music.ui2.BaseActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/20/14.
 */
@Module (
        injects = {
                LibraryFragment.class,
                FolderFragment.class,
                SearchFragment.class,
        },
        addsTo = AppModule.class,
        complete = false,
        includes = {
                CardModule.class,
                RemoteLibraryModule.class,
        }
)
public class LibraryModule {

    @Provides @Singleton @ForFragment
    public Bus provideEventBus() {
        return new Bus("library");
    }

}
