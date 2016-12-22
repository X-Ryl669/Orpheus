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

package org.opensilk.music.library.kutr.ui;

import android.content.Context;
import android.content.Intent;


import org.opensilk.music.library.kutr.Constants;
import org.opensilk.music.library.kutr.client.KutrAccountCredential;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Created by drew on 10/20/15.
 */
public class KutrAuthService {

    public static final String SERVICE_NAME = KutrAuthService.class.getName();

    final KutrAccountCredential credential;

    private BehaviorSubject<String> tokenSubject;

    public KutrAuthService(Context context) {
        this.credential = new KutrAccountCredential(context);//, Constants.SCOPES);
    }

    @SuppressWarnings("ResourceType")
    public static KutrAuthService getService(Context context) {
        return (KutrAuthService) context.getSystemService(SERVICE_NAME);
    }

 //   public Intent getAccountChooserIntent() {
 //       return credential.newChooseAccountIntent();
 //   }

    public Subscription getToken(final String account, final String password, final String host, Subscriber<String> subscriber, boolean force) {
        if (tokenSubject == null || force) {
            tokenSubject = BehaviorSubject.create();
            Observable.create(
                    new Observable.OnSubscribe<String>() {
                        @Override
                        public void call(Subscriber<? super String> subscriber) {
                            try {
                                Timber.d("Fetching token");
                                String token = credential.fetchToken(account, password, host);
                                Timber.d("Found token");
                                subscriber.onNext(token);
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tokenSubject);
        }
        return tokenSubject.subscribe(subscriber);
    }

    public boolean isFetchingToken() {
        return tokenSubject != null;
    }
}
