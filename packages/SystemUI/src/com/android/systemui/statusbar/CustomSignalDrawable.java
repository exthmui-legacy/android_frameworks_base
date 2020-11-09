package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.util.DarkIconUtil;

public class CustomSignalDrawable extends LayerDrawable {

    public static CustomSignalDrawable create(Context context) {
        SignalDrawable drawable = new SignalDrawable(context, context.getDrawable(R.drawable.ic_signal_cellular));
        Drawable darkDrawable = DarkIconUtil.getCustomDarkDrawable(context, R.drawable.ic_signal_cellular);
        if (darkDrawable == null) {
            return new CustomSignalDrawable(new SignalDrawable[]{drawable});
        } else {
            return new CustomSignalDrawable(new SignalDrawable[]{drawable, new SignalDrawable(context, darkDrawable)});
        }
    }

    protected CustomSignalDrawable(SignalDrawable []drawables) {
        super(drawables);

        for (int i = 0; i < drawables.length; i++) {
            drawables[i].setTintDrawable(drawables.length <= 1);
            setLayerGravity(i, Gravity.CENTER);
        }
        mutate();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        if (getNumberOfLayers() > 1) {
            if (DarkIconUtil.isDark(tint.getDefaultColor())) {
                getDrawable(0).setAlpha(0);
                getDrawable(1).setAlpha(255);
            } else {
                getDrawable(1).setAlpha(0);
                getDrawable(0).setAlpha(255);
            }
        }
        invalidateSelf();
    }

}
