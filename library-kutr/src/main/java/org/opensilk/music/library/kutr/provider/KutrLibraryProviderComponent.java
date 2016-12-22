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

package org.opensilk.music.library.kutr.provider;

import org.opensilk.music.library.kutr.KutrLibraryComponent;
import org.opensilk.music.library.kutr.client.KutrClientComponent;
import org.opensilk.music.library.kutr.client.KutrClientModule;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 10/20/15.
 */
@KutrLibraryProviderScope
@Component(
        dependencies = {
                KutrLibraryComponent.class,
        },
        modules = {
                KutrLibraryProviderModule.class
        }
)
public interface KutrLibraryProviderComponent {
        Func2<KutrLibraryComponent, KutrLibraryProviderModule, KutrLibraryProviderComponent> FACTORY =
                new Func2<KutrLibraryComponent, KutrLibraryProviderModule, KutrLibraryProviderComponent>() {
                        @Override
                        public KutrLibraryProviderComponent call(KutrLibraryComponent kutrComponent, KutrLibraryProviderModule kutrLibraryModule) {
                                return DaggerKutrLibraryProviderComponent.builder()
                                        .kutrLibraryComponent(kutrComponent)
                                        .kutrLibraryProviderModule(kutrLibraryModule)
                                        .build();
                        }
                };
        void inject(KutrLibraryProvider provider);
        KutrClientComponent kutrClientComponent(KutrClientModule module);
}
