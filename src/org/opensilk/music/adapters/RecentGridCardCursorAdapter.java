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

package org.opensilk.music.adapters;

import android.content.Context;
import android.database.Cursor;

import org.opensilk.music.loaders.RecentCursorLoader;
import org.opensilk.music.ui.cards.CardRecentGrid;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridCursorAdapter;

/**
 * Created by drew on 2/18/14.
 */
public class RecentGridCardCursorAdapter extends CardGridCursorAdapter {

    public RecentGridCardCursorAdapter(Context context) {
        super(context);
    }

    @Override
    protected Card getCardFromCursor(Cursor cursor) {
        return new CardRecentGrid(getContext(), CursorHelpers.makeAlbumFromCursor(cursor));
    }
}