/*
 * Copyright (C) 2017 The Android Open Source Project
 *               2019-2020 The exTHmUI Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.service.quicksettings.Tile;
import android.widget.ImageView;

import com.android.settingslib.graph.SignalDrawable;
import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.Icon;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.NeutralGoodDrawable;

import java.util.Objects;

// Exists to provide easy way to add sim icon to cell tile
// TODO Find a better way to handle this and remove it.
public class CellTileView extends SignalTileView {

    private final SignalDrawable mSignalDrawable;
    private NeutralGoodDrawable mCustomSignalDrawable;
    private boolean mCustomizeSignalIcon;
    private int foregroundColor;
    private int backgroundColor;
    private int iconSize;
    private Context mContext;

    public CellTileView(Context context) {
        super(context);
        
        mContext = context;
        mSignalDrawable = new SignalDrawable(context);
        mSignalDrawable.setColors(QSTileImpl.getColorForState(context, Tile.STATE_UNAVAILABLE),
                QSTileImpl.getColorForState(context, Tile.STATE_ACTIVE));
        mSignalDrawable.setIntrinsicSize(context.getResources().getDimensionPixelSize(
                R.dimen.qs_tile_icon_size));
        mCustomizeSignalIcon = context.getResources().getBoolean(R.bool.customize_mobile_signal_icon);
        backgroundColor = QSTileImpl.getColorForState(context, Tile.STATE_UNAVAILABLE);
        foregroundColor = QSTileImpl.getColorForState(context, Tile.STATE_ACTIVE);
        iconSize = context.getResources().getDimensionPixelSize(R.dimen.qs_tile_icon_size);
    }

    protected void updateIcon(ImageView iv, State state) {
        if (!(state.icon instanceof SignalIcon)) {
            super.updateIcon(iv, state);
            return;
        } else if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            if (mCustomizeSignalIcon) {
                mCustomSignalDrawable = NeutralGoodDrawable.create(mContext, ((SignalIcon) state.icon).getState());
                iv.setImageDrawable(mCustomSignalDrawable);
                iv.setBackgroundColor(backgroundColor);
            } else {
                mSignalDrawable.setLevel(((SignalIcon) state.icon).getState());
                iv.setImageDrawable(mSignalDrawable);
            }
            
            iv.setTag(R.id.qs_icon_tag, state.icon);
        }
    }

    public static class SignalIcon extends Icon {

        private final int mState;

        public SignalIcon(int state) {
            mState = state;
        }

        public int getState() {
            return mState;
        }

        @Override
        public Drawable getDrawable(Context context) {
            //TODO: Not the optimal solution to create this drawable
            boolean customizeSignalIcon = context.getResources().getBoolean(R.bool.customize_mobile_signal_icon);
            if (customizeSignalIcon) {
                NeutralGoodDrawable d = NeutralGoodDrawable.create(context, getState());
                return d;
            } else {
                SignalDrawable d = new SignalDrawable(context);
                d.setColors(QSTileImpl.getColorForState(context, Tile.STATE_UNAVAILABLE),
                    QSTileImpl.getColorForState(context, Tile.STATE_ACTIVE));
                d.setLevel(getState());
                return d;
            }
                
        }
    }
}
