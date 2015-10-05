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

package de.umass.lastfm;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by drew on 9/16/15.
 */

public interface LastFM {
    String API_ROOT = "https://ws.audioscrobbler.com/2.0/";

    @GET("?method=artist.getInfo")
    Call<Artist> getArtist(@Query("artist") String artist);

    @GET("?method=artist.getInfo")
    Observable<Artist> getArtistObservable(@Query("artist") String artist);

    @GET("?method=album.getInfo")
    Call<Album> getAlbum(@Query("artist") String artist, @Query("album") String album);

    @GET("?method=album.getInfo")
    Observable<Album> getAlbumObservable(@Query("artist") String artist, @Query("album") String album);
}
