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

package org.opensilk.music.ui3.playlist;

import android.content.Context;
import android.view.ViewGroup;

import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.MortarPagerAdapter;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerList;

import java.util.List;
import java.util.Locale;

/**
 * Created by drew on 12/11/15.
 */
class PlaylistPagerScreenViewAdapter extends MortarPagerAdapter<PlaylistsScreen, BundleableRecyclerList> {

    final PlaylistPagerScreenPresenter mPresenter;

    private Object mCurrentPrimaryItem;

    public PlaylistPagerScreenViewAdapter(Context context, List<PlaylistsScreen> pages, PlaylistPagerScreenPresenter presenter) {
        super(context, pages);
        mPresenter = presenter;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return screens.get(position).getTitle().toUpperCase(Locale.getDefault());
    }

    @Override @SuppressWarnings("unchecked")
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object != mCurrentPrimaryItem) {
            Page<Screen, BundleableRecyclerList> currentPage = (Page<Screen, BundleableRecyclerList>) object;
            BundleablePresenter childPresenter = currentPage.view.getPresenter();
            ActionBarMenuHandler menuConfig = null;
            if (childPresenter != null) {
                menuConfig = childPresenter.getMenuConfig();
            }
            mPresenter.updateActionBarWithChildMenuConfig(menuConfig);
            mCurrentPrimaryItem = object;
        }
    }
}
