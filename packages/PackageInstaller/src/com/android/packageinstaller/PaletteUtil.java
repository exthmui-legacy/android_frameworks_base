package com.android.packageinstaller;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class PaletteUtil {

    public static Bitmap getIconBitmap(Drawable drawable) {
        try {
            if (drawable == null) {
                return null;
            }
            if (drawable instanceof AdaptiveIconDrawable) {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            } else {
                return ((BitmapDrawable) drawable).getBitmap();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static int ColorBurn(int RGBValues) {
        int red = RGBValues >> 16 & 0xFF;
        int green = RGBValues >> 8 & 0xFF;
        int blue = RGBValues & 0xFF;
        red = (int) Math.floor(red * (1 - 0.1));
        green = (int) Math.floor(green * (1 - 0.1));
        blue = (int) Math.floor(blue * (1 - 0.1));
        return Color.rgb(red, green, blue);
    }

    static int toMaxAlpha(int color) {
        return Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

}
