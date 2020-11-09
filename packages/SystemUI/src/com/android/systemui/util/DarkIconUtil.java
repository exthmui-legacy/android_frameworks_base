package com.android.systemui.util;

import android.annotation.DrawableRes;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.Arrays;
import java.util.List;

import com.android.systemui.R;

public class DarkIconUtil {

    public static Drawable getCustomDarkDrawable(Context context, @DrawableRes int iconResId) {
        try {
            Resources resources = context.getResources();
            String[] customArr = resources.getStringArray(R.array.custom_dark_icons);
            List<String> customDarkIcons = Arrays.asList(customArr);
            String resName = resources.getResourceEntryName(iconResId);
            if (customDarkIcons.contains(resName)) {
                int darkResId = resources.getIdentifier(resName + "_dark", "drawable", context.getPackageName());
                if (darkResId != 0) {
                    return resources.getDrawable(darkResId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isDark(int color) {
        return Color.luminance(color) < 0.5;
    }
}
