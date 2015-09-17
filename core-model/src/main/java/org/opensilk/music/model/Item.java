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

package org.opensilk.music.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.opensilk.music.model.spi.Bundleable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Describes a single object
 *
 * Created by drew on 9/3/15.
 */
public abstract class Item implements Bundleable {

    protected final Uri uri;
    protected final String name;
    protected final Metadata metadata;

    protected Item(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        this.uri = uri;
        this.name = name;
        this.metadata = metadata;
    }

    @Override @Deprecated
    public String getIdentity() {
        return uri.toString();
    }

    @Override
    public String getName() {
        return getDisplayName();
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public String getDisplayName() {
        String disName = metadata.getString(Metadata.KEY_DISPLAY_NAME);
        return disName != null ? disName : name;
    }

    public Uri getParentUri() {
        return metadata.getUri(Metadata.KEY_PARENT_URI);
    }

    public long getLastModified() {
        return metadata.getLong(Metadata.KEY_LAST_MODIFIED);
    }

    public String getMimeType() {
        return metadata.getString(Metadata.KEY_MIME_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return uri.equals(item.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}
