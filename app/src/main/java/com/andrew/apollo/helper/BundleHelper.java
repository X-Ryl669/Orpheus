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

package com.andrew.apollo.helper;

import android.os.Bundle;

/**
 * Created by drew on 4/24/15.
 */
public class BundleHelper {
    public static final String INT_ARG = "intarg";

    public static int getInt(Bundle b) {
        return b.getInt(INT_ARG);
    }

    public static BundleHelper.Builder builder() {
        return new BundleHelper.Builder(new Bundle());
    }

    /**
     * Wraps bundle for chaining.
     */
    public static class Builder {
        final Bundle b;

        private Builder(Bundle b) {
            this.b = b;
        }

        public Builder putInt(int val) {
            b.putInt(INT_ARG, val);
            return this;
        }

        public Bundle get() {
            return b;
        }
    }




}
