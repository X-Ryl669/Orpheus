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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;
import com.jakewharton.rxbinding.view.ViewLongClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.ForegroundRelativeLayout;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/17/15.
 */
public class FooterPageScreenView extends ForegroundRelativeLayout {

    @Inject FooterPageScreenPresenter mPresenter;

    @InjectView(R.id.footer_track_title) TextView trackTitle;
    @InjectView(R.id.footer_artist_name) TextView artistName;
    @InjectView(R.id.footer_thumbnail) AnimatedImageView artworkThumbnail;
    @InjectView(R.id.footer_playpause_btn) ImageButton mPlayPause;

    CompositeSubscription mSubs = new CompositeSubscription();

    public FooterPageScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        FooterPageScreenComponent cmp = DaggerService.getDaggerComponent(getContext());
        cmp.inject(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);
        mPresenter.takeView(this);
        subscribeClicks();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    void setPlaying(boolean yes) {
        mPlayPause.setImageLevel(yes ? 1 : 0);
    }

    void subscribeClicks() {
        mSubs.add(RxView.clickEvents(this).subscribe(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent viewClickEvent) {
                mPresenter.onClick(viewClickEvent.view());
            }
        }));
        mSubs.add(RxView.longClickEvents(this, new Func1<ViewLongClickEvent, Boolean>() {
            @Override
            public Boolean call(ViewLongClickEvent viewLongClickEvent) {
                return mPresenter.onLongClick(viewLongClickEvent.view());
            }
        }).subscribe());
        mSubs.add(RxView.clickEvents(mPlayPause).subscribe(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent viewClickEvent) {
                mPresenter.togglePlayback();
            }
        }));
    }
}
