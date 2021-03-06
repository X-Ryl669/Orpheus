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

package org.opensilk.music;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.iab.gplay.IabGplayModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/16/14.
 */
@Module (
        library = true,
        addsTo = GlobalModule.class,
        injects = MusicApp.class,
        includes = {
                IabGplayModule.class,
        }
)
public class AppModule {

    private final MusicApp app;

    public AppModule(MusicApp app) {
        this.app = app;
    }

    @Provides @Singleton @ForApplication
    public MusicApp provideApplication() {
        return app;
    }

}
