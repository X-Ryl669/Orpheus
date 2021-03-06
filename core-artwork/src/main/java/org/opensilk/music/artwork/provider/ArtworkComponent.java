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

package org.opensilk.music.artwork.provider;

import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.common.core.dagger2.SystemServicesComponent;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.coverartarchive.CoverArtArchiveComponent;
import org.opensilk.music.artwork.shared.ArtworkComponentCommon;
import org.opensilk.music.lastfm.LastFMComponent;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import rx.Scheduler;

/**
 * Root component for provider process, This isnt used directly but extended to allow
 * additional providers to be used. Annotations for documentation and clarity.
 *
 * Created by drew on 5/1/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                ArtworkModule.class,
        }
)
public interface ArtworkComponent extends ArtworkComponentCommon,
        SystemServicesComponent, LastFMComponent, CoverArtArchiveComponent {
    void inject(ArtworkProvider provider);
    BitmapDiskCache bitmapDiskCache();
    @Named("artworkscheduler") Scheduler artworkScheduler();
}
