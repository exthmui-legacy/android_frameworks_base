package com.android.keyguard.clock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.ClockPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class JavaClockController implements ClockPlugin {

    /**
     * Resources used to get title and thumbnail.
     */
    private final Resources mResources;

    /**
     * LayoutInflater used to inflate custom clock views.
     */
    private final LayoutInflater mLayoutInflater;

    /**
     * Renders preview from clock view.
     */
    private final ViewPreviewer mRenderer = new ViewPreviewer();

    /**
     * Root view of clock.
     */
    private ClockLayout mView;

    /**
     * Text clock in preview view hierarchy.
     */
    private TextView mClock;

    /**
     * Create a DefaultClockController instance.
     *
     * @param res            Resources contains title and thumbnail.
     * @param inflater       Inflater used to inflate custom clock views.
     * @param colorExtractor Extracts accent color from wallpaper.
     */
    public JavaClockController(Resources res, LayoutInflater inflater,
                               SysuiColorExtractor colorExtractor) {
        mResources = res;
        mLayoutInflater = inflater;
    }

    private void createViews() {
        mView = (ClockLayout) mLayoutInflater
                .inflate(R.layout.java_clock, null);
        mClock = mView.findViewById(R.id.clock);
        setText(mClock);
    }

    @Override
    public void onDestroyView() {
        mView = null;
        mClock = null;
    }

    @Override
    public String getName() {
        return "javaClock";
    }

    @Override
    public String getTitle() {
        return mResources.getString(R.string.clock_title_java);
    }

    @Override
    public int getPreferredY(int totalHeight) {
        return totalHeight / 3;
    }

    @Override
    public Bitmap getThumbnail() {
        return BitmapFactory.decodeResource(mResources, R.drawable.java_thumbnail);
    }

    @Override
    public Bitmap getPreview(int width, int height) {
        View previewView = getView();
        TextView textView = previewView.findViewById(R.id.clock);
        setTextColor(Color.WHITE);
        setText(textView);

        onTimeTick();
        return mRenderer.createPreview(previewView, width, height);
    }

    @Override
    public View getView() {
        if (mView == null) {
            createViews();
        }
        return mView;
    }

    private void setText(TextView textView) {
        Locale locale = new Locale(Locale.getDefault().getLanguage());
        String format;
        if (DateFormat.is24HourFormat(textView.getContext())) {
            format = "HH:mm";
        } else {
            format = "hh:mm";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, locale);
        String targetTime = simpleDateFormat.format(new Date());
        textView.setText(mResources.getString(R.string.java_clock_text, targetTime));
    }

    @Override
    public View getBigClockView() {
        return null;
    }

    @Override
    public void setStyle(Paint.Style style) {
    }

    @Override
    public void setTextColor(int color) {
        mClock.setTextColor(color);
    }

    @Override
    public void setColorPalette(boolean supportsDarkText, int[] colorPalette) {
    }

    @Override
    public void onTimeTick() {
        mView.onTimeChanged();
        setText(mClock);
    }

    @Override
    public void setDarkAmount(float darkAmount) {
        mView.setDarkAmount(darkAmount);
    }

    @Override
    public void onTimeZoneChanged(TimeZone timeZone) {
    }

    @Override
    public boolean shouldShowStatusArea() {
        return true;
    }
}