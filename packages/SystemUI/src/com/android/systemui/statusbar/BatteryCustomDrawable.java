/*
 * Copyright (C) 2019-2020 The exTHmUI Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import com.android.settingslib.Utils;
import com.android.systemui.R;

public class BatteryCustomDrawable extends LayerDrawable {

    private int mBatteryLevel = 100;
    private int mBatteryStatus = 0;
    private float mDarkIntensity = 0;
    private boolean mCharging;
    private boolean mPowerSave;

    public static BatteryCustomDrawable create(Context context, int normalResId, int chargingResId, int powersaveResId) {
        int dualToneLightTheme = Utils.getThemeAttr(context, R.attr.lightIconTheme);
        int dualToneDarkTheme = Utils.getThemeAttr(context, R.attr.darkIconTheme);
        ContextThemeWrapper light = new ContextThemeWrapper(context, dualToneLightTheme);
        ContextThemeWrapper dark = new ContextThemeWrapper(context, dualToneDarkTheme);
        return new BatteryCustomDrawable(
                new Drawable[] {
                        light.getDrawable(normalResId).mutate(),
                        dark.getDrawable(NeutralGoodDrawable.getDarkResId(dark, normalResId)).mutate(),
                        light.getDrawable(chargingResId).mutate(),
                        dark.getDrawable(NeutralGoodDrawable.getDarkResId(dark, chargingResId)).mutate(),
                        light.getDrawable(powersaveResId).mutate(),
                        dark.getDrawable(NeutralGoodDrawable.getDarkResId(dark, powersaveResId)).mutate()
                });
    }

    protected BatteryCustomDrawable(Drawable []drawables) {
        super(drawables);

        for (int i = 0; i < drawables.length; i++) {
            setLayerGravity(i, Gravity.CENTER);
        }

        mutate();
        updateBatteryStatus();
        setDarkIntensity(0);
    }

    public void setCharging(boolean charging) {
        mCharging = charging;
        updateBatteryStatus();
    }

    public void setPowerSave(boolean powerSave) {
        mPowerSave = powerSave;
        updateBatteryStatus();
    }

    public void updateBatteryStatus()
    {
        if (mCharging) {
            mBatteryStatus = 1;
        } else if (mPowerSave) {
            mBatteryStatus = 2;
        } else {
            mBatteryStatus = 0;
        }
        for (int i = 0; i < 6; i++) {
            getDrawable(i).setAlpha(0);
        }

        setBatteryLevel(mBatteryLevel);
        setDarkIntensity(mDarkIntensity);
    }


    public void setBatteryLevel(int batteryLevel)
    {
        mBatteryLevel = batteryLevel;
        getDrawable(mBatteryStatus * 2).setLevel(batteryLevel);
        getDrawable(mBatteryStatus * 2 + 1).setLevel(batteryLevel);
        invalidateSelf();
    }

    public void setDarkIntensity(float intensity) {
        mDarkIntensity = intensity;
        getDrawable(mBatteryStatus * 2).setAlpha((int) ((1 - intensity) * 255f));
        getDrawable(mBatteryStatus * 2 + 1).setAlpha((int) (intensity * 255f));
        invalidateSelf();
    }
}
