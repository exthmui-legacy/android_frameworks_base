/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.android.packageinstaller;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.packageinstaller.utils.ContentUriUtils;
import com.android.packageinstaller.utils.NotificationUtil;

import java.io.File;
import java.util.List;

/**
 * Finish installation: Return status code to the caller or display "success" UI to user
 */
public class InstallSuccess extends Activity {
    private static final String LOG_TAG = InstallSuccess.class.getSimpleName();

    private String mFromSource;
    private String mVersionName;

    private TextView mAppLabelView;
    private TextView mFromSourceView;
    private TextView mVersionNameView;

    private CardView mDeleteApkLayout;
    private CardView mAppInfoContainer;
    private Button mInstallButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.install_main);

        mFromSource = getIntent().getStringExtra("key_fromSource");
        mVersionName = getIntent().getStringExtra("key_versionName");

        mAppLabelView = findViewById(R.id.app_name);
        mFromSourceView = findViewById(R.id.from_source);
        mVersionNameView = findViewById(R.id.app_versionName);

        mAppInfoContainer = findViewById(R.id.app_info_container);
        mDeleteApkLayout = findViewById(R.id.delete_apk_view);
        mInstallButton = findViewById(R.id.install_button);

        if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
            // Return result if requested
            Intent result = new Intent();
            result.putExtra(Intent.EXTRA_INSTALL_RESULT, PackageManager.INSTALL_SUCCEEDED);
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            Intent intent = getIntent();
            ApplicationInfo appInfo =
                    intent.getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
            Uri packageURI = intent.getData();

            // Set header icon and title
            PackageUtil.AppSnippet as;
            PackageManager pm = getPackageManager();

            CharSequence versionName = null;
            try {
                versionName = pm.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if ("package".equals(packageURI.getScheme())) {
                as = new PackageUtil.AppSnippet(pm.getApplicationLabel(appInfo), versionName,
                        pm.getApplicationIcon(appInfo));
            } else {
                File sourceFile = new File(packageURI.getPath());
                as = PackageUtil.getAppSnippet(this, appInfo, sourceFile);
            }

            //TODO: 完成翻译
            if (getIntent().getBooleanExtra("DELETE_APK_ENABLE", false)) {
                Uri dataUri = Uri.parse(getIntent().getStringExtra("ORIGINAL_LOCATION"));
                File apkfile;
                try {
                    getContentResolver().delete(dataUri, null, null);
                    Toast.makeText(this, appInfo.packageName + "安装完成，" + "已清理" + Formatter.formatFileSize(this, new File(intent.getData().getPath()).length()), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(this.getClass().getSimpleName(), dataUri.getAuthority());
                    try {
                        apkfile = new File(ContentUriUtils.getPath(this, dataUri));
                        apkfile.delete();
                        Toast.makeText(this, appInfo.packageName + "安装完成，" + "已清理" + Formatter.formatFileSize(this, new File(intent.getData().getPath()).length()), Toast.LENGTH_SHORT).show();
                    } catch (Exception e1) {
                        //Ignore
                    }
                }
            }

            ImageView app_icon = findViewById(R.id.app_icon);
            app_icon.setImageDrawable(as.icon);
            mAppLabelView.setText(as.label);
            mFromSourceView.setText(mFromSource);
            mVersionNameView.setText(mVersionName);

            Palette.from(PaletteUtil.getIconBitmap(as.icon)).generate(palette1 -> {
                int defaultColor = 0x5eb5f7;
                int darkVibrantColor = palette1.getDarkVibrantColor(defaultColor);
                int lightVibrantColor = palette1.getLightVibrantColor(defaultColor);
                int darkMutedColor = palette1.getDarkMutedColor(defaultColor);
                int lightMutedColor = palette1.getLightMutedColor(defaultColor);
                int vibrantColor = palette1.getVibrantColor(defaultColor);
                int mutedColor = palette1.getMutedColor(defaultColor);
                mAppInfoContainer.setCardBackgroundColor(PaletteUtil.ColorBurn(lightVibrantColor));
                mDeleteApkLayout.setCardBackgroundColor(PaletteUtil.ColorBurn(lightVibrantColor));
                mInstallButton.setBackgroundColor(PaletteUtil.ColorBurn(lightVibrantColor));
            });

            Button mInstallButton = findViewById(R.id.install_button);
            Button mCancelButton = findViewById(R.id.cancel_button);
            mInstallButton.setText(getString(R.string.launch));
            mCancelButton.setText(getString(R.string.done));
            mInstallButton.setOnClickListener(null);
            mCancelButton.setOnClickListener(view -> {
                if (appInfo.packageName != null) {
                    Log.i(LOG_TAG, "Finished installing " + appInfo.packageName);
                }
                finish();
            });
            LinearLayout mInstallResultContainer = findViewById(R.id.install_result_container);
            ImageView mInstallResultImage = findViewById(R.id.install_result_image);
            mInstallResultImage.setImageResource(R.drawable.ic_install_done);
            mInstallResultContainer.setVisibility(View.VISIBLE);
            TextView mInstallResult = requireViewById(R.id.install_result);
            mInstallResult.setText(getString(R.string.install_done));
            // Enable or disable "launch" button
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
                    appInfo.packageName);
            boolean enabled = false;
            PendingIntent pendingIntent;
            String content;
            if (launchIntent != null) {
                content = getString(R.string.launch);
                pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                List<ResolveInfo> list = getPackageManager().queryIntentActivities(launchIntent,
                        0);
                if (list != null && list.size() > 0) {
                    enabled = true;
                }
            } else {
                content = getString(R.string.install_done);
                pendingIntent = null;
            }
            //创建Notification通知  start
            String title = as.label.toString();

            NotificationManager notificationManager =
                    this.getSystemService(NotificationManager.class);
            int installId = intent.getIntExtra("com.android.packageinstaller.extra.INSTALL_ID", 0);

            notificationManager.notify(installId, NotificationUtil.buildNotification(this, pendingIntent, "id1", title, content, R.drawable.ic_packageinstaller_logo, as.label));
            //创建Notification通知  end

            Button launchButton = mInstallButton;
            if (enabled) {
                launchButton.setOnClickListener(view -> {
                    try {
                        startActivity(launchIntent);
                    } catch (ActivityNotFoundException | SecurityException e) {
                        Log.e(LOG_TAG, "Could not start activity", e);
                    }
                    finish();
                });
            } else {
                launchButton.setEnabled(false);
            }
        }
    }
}
