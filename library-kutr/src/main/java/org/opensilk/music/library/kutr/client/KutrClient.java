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

package org.opensilk.music.library.kutr.client;

import android.net.Uri;

/*
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
*/
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.library.kutr.provider.KutrLibraryProvider;
import org.opensilk.music.library.kutr.provider.KutrLibraryUris;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

//import static org.opensilk.music.library.kutr.Constants.BASE_QUERY;
//import static org.opensilk.music.library.kutr.Constants.LIST_FIELDS;
import static org.opensilk.music.library.kutr.client.ModelUtil.IS_AUDIO;
import static org.opensilk.music.library.kutr.client.ModelUtil.IS_FOLDER;
import static org.opensilk.music.library.kutr.client.ModelUtil.IS_IMAGE;
import static org.opensilk.music.library.kutr.client.ModelUtil.buildDownloadUri;
import static org.opensilk.music.library.kutr.client.ModelUtil.formatDate;
import static org.opensilk.music.library.kutr.client.ModelUtil.pickSuitableImage;
import static org.opensilk.music.library.internal.LibraryException.Kind.AUTH_FAILURE;
import static org.opensilk.music.library.internal.LibraryException.Kind.NETWORK;
import static org.opensilk.music.library.internal.LibraryException.Kind.UNKNOWN;

/**
 * Created by drew on 10/20/15.
 */
@KutrClientScope
public class KutrClient {
//    final Kutr mKutr;
    final KutrAccountCredential mCredential;
    final String mAuthority;
    final String mAccount;

    @Inject
    public KutrClient(
            HttpTransport httpTransport,
            JsonFactory jsonFactory,
            KutrAccountCredential credential,
            @Named("AppIdentifier") String appIdentifier,
            @Named("kutrLibraryAuthority") String authority
    ) {
        /*
        mKutr = new Kutr.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(appIdentifier).build();
        */
        mCredential = credential;
        mAuthority = authority;
        mAccount = credential.getSelectedAccountName();
    }

    public Observable<Model> listFolder(final String identity) {
        return null; //TODO
        /*
        final String q = "'" + identity + "'" + BASE_QUERY;
        return getFiles(q)
                .collect(collectorFactory, collectAction)
                .flatMap(new Func1<Collector, Observable<Model>>() {
                    @Override
                    public Observable<Model> call(Collector collector) {
                        Uri artworkUri = null;
                        if (!collector.images.isEmpty()) {
                            Timber.d("Found %d images in folder", collector.images.size());
                            File artwork = pickSuitableImage(collector.images);
                            if (artwork != null) {
                                Timber.d("Found suitable coverart %s", artwork.getTitle());
                                artworkUri = buildDownloadUri(artwork.getDownloadUrl(), collector.token);
                            }
                        }
                        List<Model> models = new ArrayList<Model>(collector.files.size() + collector.folders.size());
                        for (File fldr : collector.folders) {
                            models.add(buildFolder(identity, fldr));
                        }
                        for (File file : collector.files) {
                            models.add(buildTrack(identity, collector.token, artworkUri, file));
                        }
                        return Observable.from(models);
                    }
                });
         */
    }

    public Observable<Model> getFolder(final String identity) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
//                try {
                    Item item = null; // mKutr.files().get(identity).execute();
                    if (item != null && !subscriber.isUnsubscribed()) {
                        if (IS_FOLDER.call(item)) {
                            subscriber.onNext(buildFolder(null, item));
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new IllegalArgumentException("Unknown file " + identity));
                        }
                    } else if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(new IllegalArgumentException("Unknown file " + identity));
                    }
//                } catch (IOException e) {
//                    handleException(e, subscriber);
//                }
            }
        });
    }

    //TODO look into batchrequest for getting file info and artwork in one call
    public Observable<Model> getFile(final String parentId, final String identity) {
        //We just reuse this as it is probably already cached
        return listFolder(parentId)
                .takeFirst(new Func1<Model, Boolean>() {
                    @Override
                    public Boolean call(Model model) {
                        return (model instanceof Track)
                                && StringUtils.equals(KutrLibraryUris.extractFileId(model.getUri()), identity);
                    }
                });
    }

    @DebugLog
    Observable<List<Item>> getFiles(final String query) {
        return null;
        /*
        return Observable.create(new Observable.OnSubscribe<List<File>>() {
            @Override
            public void call(Subscriber<? super List<File>> subscriber) {
                try {
                    String paginationToken = null;
                    do {
                        Timber.d("q=%s", query);
                        if (subscriber.isUnsubscribed()) {
                            return; //Shortcircuit if nobody listening
                        }
                        Kutr.Files.List req = mKutr.files().list()
                                .setQ(query)
                                .setFields(LIST_FIELDS);
                        if (!StringUtils.isEmpty(paginationToken)) {
                            req.setPageToken(paginationToken);
                        }
                        FileList resp = req.execute();
                        Timber.v(ReflectionToStringBuilder.toString(resp, RecursiveToStringStyle.MULTI_LINE_STYLE));
                        //TODO cache response
                        if (!subscriber.isUnsubscribed()) {
                            List<File> files = resp.getItems();
                            if (files != null && !files.isEmpty()) {
                                subscriber.onNext(files);
                            }
                        }
                        paginationToken = resp.getNextPageToken();
                    } while (!StringUtils.isEmpty(paginationToken) && !subscriber.isUnsubscribed());
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    handleException(e, subscriber);
                }
            }
        });
        */
    }

    Folder buildFolder(String parentFolder, Item i) {
        final String id = i.getId();
        final Uri uri = KutrLibraryUris.folder(mAuthority, mAccount, id);
        final Uri parentUri = KutrLibraryUris.folder(mAuthority, mAccount, parentFolder);
        final String title = i.getName();
        return Folder.builder()
                .setUri(uri)
                .setParentUri(parentUri)
                .setName(title)
                .build();
    }

    Track buildTrack(String parentFolder, String authToken, Uri artworkUri, Item i) {
        final String id = i.getId();
        final Uri uri = KutrLibraryUris.file(mAuthority, mAccount, parentFolder, id);
        final Uri parentUri = KutrLibraryUris.folder(mAuthority, mAccount, parentFolder);
        final String title = i.getName();
        final Track.Builder bob = Track.builder()
                .setUri(uri)
                .setParentUri(parentUri)
                .setName(title)
                .setArtworkUri(artworkUri)//Null ok
                ;
        final Uri data = Uri.parse(i.getDownloadUrl());// buildDownloadUri(f.getDownloadUrl(), authToken);
    //    final long size = i.getFileSize();
        final String mimeType = i.getMimeType();
        final Track.Res res = Track.Res.builder()
                .setUri(data)
                .addHeader("Authorization", "Bearer " + authToken)
  //              .setLastMod(lastMod)
  //              .setSize(size)
                .setMimeType(mimeType)
                .build();
        return bob.addRes(res).build();
    }

    void removeAccount() {
//        mCredential.getContext().getContentResolver().call(LibraryUris.call(mAuthority),
//                KutrLibraryProvider.INVALIDATE_ACCOUNT, null, BundleHelper.b().putString(mAccount).get());
        //TODO
    }

    void handleException(Throwable e, Subscriber<?> subscriber) {
        Timber.e(e, "handleException");
        LibraryException ex = null;
        if (e instanceof KutrAuthException) {
            ex = new LibraryException(AUTH_FAILURE, e);
        } else if (e instanceof IOException) {
            ex = new LibraryException(NETWORK, e);
        } else {
            ex = new LibraryException(UNKNOWN, e);
        }
        if (subscriber != null && !subscriber.isUnsubscribed()) {
            subscriber.onError(ex);
        }
    }

    final Func0<Collector> collectorFactory = new Func0<Collector>() {
        @Override
        public Collector call() {
            try {
                String token = mCredential.getToken();
                return new Collector(token);
            } catch (KutrAuthException e) {
                removeAccount();
                throw Exceptions.propagate(new LibraryException(LibraryException.Kind.AUTH_FAILURE, e));
            } catch (IOException e) {
                throw Exceptions.propagate(new LibraryException(LibraryException.Kind.NETWORK, e));
            }
        }
    };

    final Action2<Collector, List<Item>> collectAction = new Action2<Collector, List<Item>>() {
        @Override
        public void call(Collector collector, List<Item> items) {
            for (Item file : items) {
                if (IS_AUDIO.call(file)) {
                    collector.songs.add(file);
                } else if (IS_FOLDER.call(file)) {
                    collector.folders.add(file);
                } else if (IS_IMAGE.call(file)) {
                    collector.images.add(file);
                }
            }
        }
    };

    static class Collector {
        final ArrayList<Item> images = new ArrayList<>();
        final ArrayList<Item> folders = new ArrayList<>();
        final ArrayList<Item> songs = new ArrayList<>();
        final String token;
        public Collector(String token) {
            this.token = token;
        }
    }

}