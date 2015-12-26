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

package org.opensilk.music.ui3.gallery;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.ActionModePresenter;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import mortar.ViewPresenter;

/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class GalleryScreenPresenter extends ViewPresenter<GalleryScreenView> {

    final AppPreferences preferences;
    final GalleryScreen screen;
    final ActionModePresenter actionModePresenter;
    final String indexAuthority;

    DelegateActionHandler delegateActionHandler;

    @Inject
    public GalleryScreenPresenter(
            AppPreferences preferences,
            GalleryScreen screen,
            ActionModePresenter actionModePresenter,
            @Named("IndexProviderAuthority") String indexAuthority
    ) {
        this.preferences = preferences;
        this.screen = screen;
        this.actionModePresenter = actionModePresenter;
        this.indexAuthority = indexAuthority;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        // init pager
//        List<GalleryPage> galleryPages = preferences.getGalleryPages();
        List<GalleryPage> galleryPages = Arrays.asList(GalleryPage.values());
        List<GalleryPageScreen> screens = new ArrayList<>(galleryPages.size());
        for (GalleryPage page : galleryPages) {
            switch (page) {
                case ALBUM:
                    screens.add(page.FACTORY.call(IndexUris.albums(indexAuthority)));
                    break;
                case ARTIST:
                    screens.add(page.FACTORY.call(IndexUris.albumArtists(indexAuthority)));
                    break;
                case GENRE:
                    screens.add(page.FACTORY.call(IndexUris.genres(indexAuthority)));
                    break;
                case SONG:
                    screens.add(page.FACTORY.call(IndexUris.tracks(indexAuthority)));
                    break;
                case FOLDER:
                    screens.add(page.FACTORY.call(IndexUris.folders(indexAuthority)));
                    break;
            }
        }
        int startPage = preferences.getInt(AppPreferences.GALLERY_START_PAGE, AppPreferences.DEFAULT_PAGE);
        getView().setup(screens, startPage);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (hasView()) {
            int pos = getView().mViewPager.getCurrentItem();
            preferences.putInt(AppPreferences.GALLERY_START_PAGE, pos);
        }
    }

    void updateActionBarWithChildMenuConfig(ActionBarMenuHandler menuConfig) {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }

        delegateActionHandler.setWrapped(menuConfig);

        if (hasView()) {
            getView().updateToolbar();
        }
    }

    ActionBarConfig getActionBarConfig() {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }
        return ActionBarConfig.builder()
                .setTitle(R.string.title_my_library)
                .setMenuConfig(delegateActionHandler)
                .build();
    }

    class DelegateActionHandler implements ActionBarMenuHandler {

        ActionBarMenuHandler wrapped;

        void setWrapped(ActionBarMenuHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean onBuildMenu(MenuInflater menuInflater, Menu menu) {
            menuInflater.inflate(R.menu.refresh, menu);
            if (wrapped != null) {
                wrapped.onBuildMenu(menuInflater, menu);
            }
            return true;
        }

        @Override
        public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.refresh:
                    context.getContentResolver().notifyChange(IndexUris.call(indexAuthority), null);
                    return true;
                default:
                    return wrapped != null && wrapped.onMenuItemClicked(context, menuItem);
            }
        }
    }

}