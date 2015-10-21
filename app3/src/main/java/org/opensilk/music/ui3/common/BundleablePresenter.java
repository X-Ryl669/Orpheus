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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxLoader;
import org.opensilk.common.core.rx.SimpleObserver;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.ActionModePresenter;
import org.opensilk.common.ui.mortar.HasOptionsMenu;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.music.model.Model;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.Subscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.notSubscribed;

/**
 * Created by drew on 5/2/15.
 */
@ScreenScope
public class BundleablePresenter extends Presenter<BundleableRecyclerView2>
        implements RxLoader.ContentChangedListener, HasOptionsMenu, ActionBarMenuHandler {

    protected final AppPreferences preferences;
    protected final ArtworkRequestManager requestor;
    protected final BundleableLoader loader;
    protected final FragmentManagerOwner fm;
    protected final ItemClickListener itemClickListener;
    protected final MenuHandler menuConfig;
    protected final List<Bundleable> loaderSeed;
    protected final ActionModePresenter actionModePresenter;
    protected final PlaybackController playbackController;

    protected boolean wantGrid;
    protected boolean wantsNumberedTracks;

    protected Subscription subscription;
    protected boolean adapterIsDirty;

    @Inject
    public BundleablePresenter(
            AppPreferences preferences,
            ArtworkRequestManager requestor,
            BundleableLoader loader,
            FragmentManagerOwner fm,
            BundleablePresenterConfig config,
            @Named("loader_uri") Uri uri,
            ActionModePresenter actionModePresenter,
            PlaybackController playbackController
    ) {
        this.preferences = preferences;
        this.requestor = requestor;
        this.loader = loader.setUri(uri).setSortOrder(preferences.getSortOrder(uri, config.defaultSortOrder));
        this.fm = fm;
        this.wantGrid = StringUtils.equals(preferences.getLayout(uri, config.wantsGrid), AppPreferences.GRID);
        this.itemClickListener = config.itemClickListener;
        this.menuConfig = config.menuConfig;
        this.loaderSeed = config.loaderSeed;
        this.actionModePresenter = actionModePresenter;
        this.playbackController = playbackController;
        this.wantsNumberedTracks = config.wantsNumberedTracks;
    }

    @Override
    protected BundleService extractBundleService(BundleableRecyclerView2 view) {
        return BundleService.getBundleService(view.getContext());
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        loader.addContentChangedListener(this);
    }

    @Override
    @DebugLog
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        setupRecyclerView(false);
//        if (savedInstanceState != null) {
//            addAll(BundleableUtil.unflatten(savedInstanceState));
//            getView().setListShown(true, false);
//            adapterIsDirty = true;
//        } else {
            getView().setLoading(true);
//        }
        if (notSubscribed(subscription)) {
            load();
        }
    }

    @Override
    @DebugLog
    protected void onSave(Bundle outState) {
        super.onSave(outState);
//        if (hasView()) {
//            BundleableUtil.flatten(outState, getView().getAdapter().getItems());
//        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (subscription != null) subscription.unsubscribe();
        loader.removeContentChangedListener(this);
    }

    protected void setupRecyclerView(boolean clear) {
        if (hasView()) {
            getView().setupRecyclerView();
            if (clear) {
                if (isSubscribed(subscription)) {
                    subscription.unsubscribe();
                }
                adapterIsDirty = true;
                load();
            }
        }
    }

    // reset the recyclerview for eg layoutmanager change
    public void resetRecyclerView() {
        setupRecyclerView(true);
    }

    protected void showRecyclerView() {
        if (hasView()) {
            getView().setListShown(true, true);
        }
    }

    protected void showEmptyView() {
        if (hasView()) {
            setEmptyText();
            getView().setListEmpty(true, true);
        }
    }

    protected void setEmptyText() {
        if (hasView()) {
            getView().setEmptyText(R.string.empty_music);
        }
    }

    // start the loader
    @DebugLog
    protected void load() {
        subscription = loader.getListObservable().subscribe(new SimpleObserver<List<Bundleable>>() {
            @Override
            @DebugLog
            public void onNext(List<Bundleable> bundleables) {
                if (!loaderSeed.isEmpty()) {
                    List<Bundleable> toadd = new ArrayList<>(loaderSeed.size() + bundleables.size());
                    toadd.addAll(loaderSeed);
                    toadd.addAll(bundleables);
                    addAll(toadd);
                } else {
                    addAll(bundleables);
                }
            }

            @Override
            public void onCompleted() {
                if (hasView() && getView().getAdapter().isEmpty()){
                    showEmptyView();
                }
            }
        });
    }

    // cancels any ongoing load and starts a new one
    @DebugLog
    public void reload() {
        if (isSubscribed(subscription)) {
            subscription.unsubscribe();
        }
        adapterIsDirty = true;
        loader.reset();
        load();
    }

    protected void addAll(Collection<Bundleable> collection) {
        if (hasView()) {
            if (adapterIsDirty) {
                adapterIsDirty = false;
                getView().notifyAdapterResetIncoming();
                getView().getAdapter().replaceAll(collection);
            } else {
                getView().getAdapter().addAll(collection);
            }
            showRecyclerView();
        }
    }

    protected void addItem(Bundleable item) {
        if (hasView()) {
            if (adapterIsDirty) {
                adapterIsDirty = false;
                getView().notifyAdapterResetIncoming();
                getView().getAdapter().clear();
            }
            getView().getAdapter().addItem(item);
            showRecyclerView();
        }
    }

    public List<Bundleable> getItems() {
        if (hasView()) {
            return getView().getAdapter().getItems();
        }
        return Collections.emptyList();
    }

    public List<Bundleable> getSelectedItems() {
        if (hasView()) {
            return getView().getAdapter().getSelectedItems();
        }
        return Collections.emptyList();
    }

    public void setWantsGrid(boolean yes) {
        wantGrid = yes;
    }

    public boolean isGrid() {
        return wantGrid;
    }

    public boolean wantsNumberedTracks() {
        return wantsNumberedTracks;
    }

    public BundleableLoader getLoader() {
        return loader;
    }

    public void onItemClicked(Context context, Bundleable item) {
        if (itemClickListener != null) itemClickListener.onItemClicked(this, context, (Model) item);
    }

    @Override
    public boolean onBuildMenu(MenuInflater menuInflater, Menu menu) {
        return menuConfig.onBuildMenu(this, menuInflater, menu);
    }

    @Override
    public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
        return menuConfig.onMenuItemClicked(this, context, menuItem);
    }

    @Override
    public ActionBarMenuHandler getMenuConfig() {
        return this;
    }

    public ArtworkRequestManager getRequestor() {
        return requestor;
    }

    public FragmentManagerOwner getFm() {
        return fm;
    }

    public AppPreferences getSettings() {
        return preferences;
    }

    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void onFabClicked(View view) {
        List<Uri> toPlay = UtilsCommon.filterTracks(getItems());
        if (toPlay.isEmpty()) {
            return; //TODO toast?
        }
        getPlaybackController().playAll(toPlay, 0);
    }

    private ActionMode actionMode;

    public void onItemClicked(BundleableRecyclerAdapter.ViewHolder viewHolder, Bundleable item) {
        if (itemClickListener != null) itemClickListener.onItemClicked(this, viewHolder.itemView.getContext(), (Model)item);
    }

    public void onItemSelected() {
        if (actionMode != null) actionMode.invalidate();
    }

    public void onItemUnselected() {
        if (actionMode != null) {
            if (getView().getAdapter().getSelectedItems().isEmpty()) {
                actionMode.finish();
            } else {
                actionMode.invalidate();
            }
        }
    }

    public void onStartSelectionMode(BundleableRecyclerAdapter.OnSelectionModeEnded callback) {
        actionMode = actionModePresenter.startActionMode(new ActionModeCallback(callback));
    }

    class ActionModeCallback implements ActionMode.Callback {
        final BundleableRecyclerAdapter.OnSelectionModeEnded callback;

        public ActionModeCallback(BundleableRecyclerAdapter.OnSelectionModeEnded callback) {
            this.callback = callback;
        }

        @Override
        @DebugLog
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitleOptionalHint(true);
            boolean started = menuConfig.onBuildActionMenu(BundleablePresenter.this,
                    mode.getMenuInflater(), menu);
            if (!started) {
                mode.finish();
            }
            return started;
        }

        @Override
        @DebugLog
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean handled = menuConfig.onActionMenuItemClicked(BundleablePresenter.this,
                    getView().getContext(), item);
            if (handled) {
                mode.finish();
            }
            return handled;
        }

        @Override
        @DebugLog
        public void onDestroyActionMode(ActionMode mode) {
            callback.onEndSelectionMode();
            actionMode = null;
        }
    }
}
