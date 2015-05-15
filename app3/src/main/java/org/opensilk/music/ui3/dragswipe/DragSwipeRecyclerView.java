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

package org.opensilk.music.ui3.dragswipe;

import android.content.Context;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.recycler.RecyclerListFrame;

/**
 * Created by drew on 5/13/15.
 */
public class DragSwipeRecyclerView extends RecyclerView {

    RecyclerView.Adapter mWrappedAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    RecyclerViewDragDropManager mRecyclerViewDragDropManager;
    RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    public DragSwipeRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setup(DragSwipeRecyclerAdapter adapter) {
        mLayoutManager = new LinearLayoutManager(getContext());

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);

        // drag & drop manager
        mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
//        mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
//                (NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z3));

        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(adapter);      // wrap for dragging
        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(mWrappedAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        setLayoutManager(mLayoutManager);
        setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        setItemAnimator(animator);

        // additional decorations
        //noinspection StatementWithEmptyBody
//        if (!VersionUtils.hasLollipop()) {
//            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
//            mList.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1)));
//        }
//        mList.addItemDecoration(new SimpleListDividerDecorator(getResources().getDrawable(R.drawable.list_divider), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(this);
        mRecyclerViewSwipeManager.attachRecyclerView(this);
        mRecyclerViewDragDropManager.attachRecyclerView(this);
    }
}
