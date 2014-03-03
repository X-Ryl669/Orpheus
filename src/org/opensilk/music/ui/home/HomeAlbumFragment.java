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
package org.opensilk.music.ui.home;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.CursorAdapter;

import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.adapters.AlbumGridCardCursorAdapter;
import org.opensilk.music.adapters.AlbumListCardCursorAdapter;
import org.opensilk.music.loaders.AlbumCursorLoader;

import static com.andrew.apollo.utils.PreferenceUtils.ALBUM_LAYOUT;

/**
 * Albums
 */
public class HomeAlbumFragment extends HomePagerBaseCursorFragment {

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumCursorLoader(getActivity());
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        if (isSimpleLayout()) {
            return new AlbumListCardCursorAdapter(getActivity());
        } else {
            return new AlbumGridCardCursorAdapter(getActivity());
        }
    }

    protected boolean isSimpleLayout() {
        return PreferenceUtils.getInstance(getActivity()).isSimpleLayout(ALBUM_LAYOUT,
                getActivity());
    }

}