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

package org.opensilk.music.ui3.library;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.DialogFactory;
import org.opensilk.music.R;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.index.client.IndexClientImpl;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.sort.FolderTrackSortOrder;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.PlayAllItemClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.Presenter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 5/2/15.
 */
@Module
public class FoldersScreenModule {
    final FoldersScreen screen;

    public FoldersScreenModule(FoldersScreen screen) {
        this.screen = screen;
    }

    @Provides
    public LibraryConfig provideLibraryConfig() {
        return screen.libraryConfig;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return screen.container.getUri();
    }

    @Provides
    public Container provideTHisContainer() {
        return screen.container;
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setToolbarTitle(screen.container.getName())
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new PlayAllItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Model item) {
                if (item instanceof Container) {
                    FoldersScreenFragment f = FoldersScreenFragment.ni(context, screen.libraryConfig, (Container)item);
                    presenter.getFm().replaceMainContent(f, true);
                } else if (item instanceof Track) {
                    super.onItemClicked(presenter, context, item);
                }
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenu(R.menu.folder_sort_by, menuInflater, menu);
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, FolderTrackSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, FolderTrackSortOrder.Z_A);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                List<Model> models = presenter.getSelectedItems();
                if (models.size() == 1) {
                    Model model = models.get(0);
                    if (model instanceof Container) {
                        if (presenter.getIndexClient().isIndexed((Container)model)) {
                            inflateMenu(R.menu.remove_from_index, menuInflater, menu);
                        } else {
                            inflateMenu(R.menu.add_to_index, menuInflater, menu);
                        }
                    }
                    if (model instanceof Track) {
                        inflateMenu(R.menu.add_to_queue, menuInflater, menu);
                        inflateMenu(R.menu.play_next, menuInflater, menu);
                    }
                    if ((model.getFlags() & LibraryConfig.FLAG_SUPPORTS_DELETE) != 0) {
                        inflateMenu(R.menu.delete, menuInflater, menu);
                    }
                    return true;
                } else {
                    return onRefreshActionMenu(presenter, menuInflater, menu);
                }
            }

            @Override
            public boolean onRefreshActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                List<Model> models = presenter.getSelectedItems();
                IndexClient indexClient = presenter.getIndexClient();
                boolean changed = false;
                MenuItem item = null;

                //add/remove

                boolean canAdd = true;
                for (Model b : models) {
                    if (b instanceof Container) {
                        if (indexClient.isIndexed((Container)b)) {
                            canAdd = false;
                            break;
                        }
                    } else {
                        canAdd = false;
                        break;
                    }
                }
                item = menu.findItem(R.id.add_to_index);
                if (item == null && canAdd) {
                    inflateMenu(R.menu.add_to_index, menuInflater, menu);
                    changed = true;
                } else if (item != null && !canAdd) {
                    menu.removeItem(R.id.add_to_index);
                    changed = true;
                }

                if (!canAdd) {
                    boolean canRemove = true;
                    for (Model b : models) {
                        if (b instanceof Container) {
                            if (!indexClient.isIndexed((Container)b)) {
                                canRemove = false;
                                break;
                            }
                        } else {
                            canRemove = false;
                            break;
                        }
                    }
                    item = menu.findItem(R.id.remove_from_index);
                    if (item == null && canRemove) {
                        inflateMenu(R.menu.remove_from_index, menuInflater, menu);
                        changed = true;
                    } else if (item != null && !canRemove) {
                        menu.removeItem(R.id.remove_from_index);
                        changed = true;
                    }
                }

                //playall

                item = menu.findItem(R.id.play_all);
                if (item == null) {
                    inflateMenu(R.menu.play_all, menuInflater, menu);
                }

                //enqueue/playnext

                boolean canEnqueue = true;
                for (Model b : models) {
                    if (!(b instanceof Track)) {
                        canEnqueue = false;
                        break;
                    }
                }
                item = menu.findItem(R.id.add_to_queue);
                if (item == null && canEnqueue) {
                    inflateMenu(R.menu.add_to_queue, menuInflater, menu);
                    changed = true;
                } else if (item != null && !canEnqueue) {
                    menu.removeItem(R.id.add_to_queue);
                    changed = true;
                }
                item = menu.findItem(R.id.play_next);
                if (item == null && canEnqueue) {
                    inflateMenu(R.menu.play_next, menuInflater, menu);
                    changed = true;
                } else if (item != null && !canEnqueue) {
                    menu.removeItem(R.id.play_next);
                    changed = true;
                }

                //delete

                boolean candelete = true;
                for (Model b : models) {
                    if ((b.getFlags() & LibraryConfig.FLAG_SUPPORTS_DELETE) == 0) {
                        candelete = false;
                        break;
                    }
                }
                item = menu.findItem(R.id.delete);
                if (item == null && candelete) {
                    inflateMenu(R.menu.delete, menuInflater, menu);
                    changed = true;
                } else if (item != null && !candelete) {
                    menu.removeItem(R.id.delete);
                    changed = true;
                }

                return changed;
            }

            @Override
            public boolean onActionMenuItemClicked(final BundleablePresenter presenter, Context context, MenuItem menuItem) {
                final IndexClient indexClient = presenter.getIndexClient();
                switch (menuItem.getItemId()) {
                    case R.id.add_to_index:
                        for (Model b : presenter.getSelectedItems()) {
                            if (b instanceof Playlist) {
                                Toast.makeText(context, R.string.msg_playlist_import_not_implemented, Toast.LENGTH_LONG).show();
                            } else if (b instanceof Container) {
                                indexClient.add((Container) b);
                            }
                        }
                        return true;
                    case R.id.remove_from_index: {
                        List<Container> containers = new ArrayList<>();
                        for (Model b : presenter.getSelectedItems()) {
                            if (b instanceof Container) {
                                containers.add((Container) b);
                            }
                        }
                        handleContainerRemoval(presenter, containers);
                        return true;
                    }
                    case R.id.play_all: {
                        playAllTracksUnderSelection(context, presenter);
                    }
                    case R.id.add_to_queue: {
                        enqueueSel(presenter.getPlaybackController(), presenter.getSelectedItems(), PlaybackConstants.ENQUEUE_LAST);
                        return true;
                    }
                    case R.id.play_next: {
                        enqueueSel(presenter.getPlaybackController(), presenter.getSelectedItems(), PlaybackConstants.ENQUEUE_NEXT);
                        return true;
                    }
                    case R.id.delete: {
                        final List<Model> models = presenter.getSelectedItems();
                        if (models.size() == 0) {
                            return true; //never happen
                        }
                        StringBuilder names = new StringBuilder(models.get(0).getName());
                        for (int ii=1; ii<models.size(); ii++) {
                            names.append(",").append(models.get(ii).getName());
                        }
                        final String title = names.toString();
                        presenter.getDialogPresenter().showDialog(new DialogFactory() {
                            @Override
                            public Dialog call(Context context) {
                                return new AlertDialog.Builder(context)
                                        .setTitle(context.getString(R.string.delete_dialog_title, title))
                                        .setMessage(R.string.cannot_be_undone)
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                indexClient.deleteItems(models, screen.container.getUri());
                                            }
                                        }).create();
                            }
                        });
                        return true;
                    }
                    default:
                        return false;

                }

            }

            void handleContainerRemoval(final BundleablePresenter presenter, final List<Container> containers) {
                Observable.create(new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        boolean allsuccess = true;
                        for (Container c : containers) {
                            if (!presenter.getIndexClient().remove(c)){
                                allsuccess = false;
                            }
                        }
                        subscriber.onNext(allsuccess);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.computation())
                        .delay(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                presenter.getDialogPresenter().dismissDialog();
                            }
                        });
                presenter.getDialogPresenter().showDialog(new DialogFactory() {
                    @Override
                    public Dialog call(Context context) {
                        ProgressDialog d = new ProgressDialog(context);
                        d.setIndeterminate(true);
                        d.setMessage(context.getString(R.string.processing));
                        return d;
                    }
                });
            }

            private void enqueueSel(PlaybackController playbackController, List<Model> selectedItems, int where) {
                List<Uri> uris = new ArrayList<>(selectedItems.size());
                for (Model b : selectedItems) {
                    uris.add(b.getUri());
                }
                playbackController.enqueueAll(uris, where);
            }
        };
    }
}
