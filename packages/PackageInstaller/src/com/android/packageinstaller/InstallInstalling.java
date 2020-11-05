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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import com.android.internal.content.PackageHelper;
import com.android.packageinstaller.utils.ContentUriUtils;
import com.android.packageinstaller.utils.NotificationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static android.content.pm.PackageInstaller.SessionParams.UID_UNKNOWN;

/**
 * Send package to the package manager and handle results from package manager. Once the
 * installation succeeds, start {@link InstallSuccess} or {@link InstallFailed}.
 * <p>This has two phases: First send the data to the package manager, then wait until the package
 * manager processed the result.</p>
 */
public class InstallInstalling extends Activity {
    private static final String LOG_TAG = InstallInstalling.class.getSimpleName();

    private static final String SESSION_ID = "com.android.packageinstaller.SESSION_ID";
    private static final String INSTALL_ID = "com.android.packageinstaller.INSTALL_ID";

    private static final String BROADCAST_ACTION =
            "com.android.packageinstaller.ACTION_INSTALL_COMMIT";

    /**
     * Listens to changed to the session and updates progress bar
     */
    private PackageInstaller.SessionCallback mSessionCallback;

    /**
     * Task that sends the package to the package installer
     */
    private InstallingAsyncTask mInstallingTask;

    /**
     * Id of the session to install the package
     */
    private int mSessionId;

    /**
     * Id of the install event we wait for
     */
    private int mInstallId;

    /**
     * URI of package to install
     */
    private Uri mPackageURI;

    /**
     * The button that can cancel this dialog
     */
    private Button mCancelButton;

    private String mFromSource;
    private String mVersionName;

    private TextView mAppLabelView;
    private TextView mFromSourceView;
    private TextView mVersionNameView;

    private CardView mDeleteApkLayout;
    private CardView mAppInfoContainer;
    private TextView mAutoDeleteApkTitle;

    private PackageUtil.AppSnippet as;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.install_main);

        mFromSource = getIntent().getStringExtra("key_fromSource");
        mVersionName = getIntent().getStringExtra("key_versionName");

        mAppLabelView = findViewById(R.id.app_name);
        mFromSourceView = findViewById(R.id.from_source);
        mVersionNameView = findViewById(R.id.app_versionName);

        ApplicationInfo appInfo = getIntent()
                .getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
        mPackageURI = getIntent().getData();
        if ("package".equals(mPackageURI.getScheme())) {
            try {
                getPackageManager().installExistingPackage(appInfo.packageName);
                launchSuccess();
            } catch (PackageManager.NameNotFoundException e) {
                launchFailure(PackageManager.INSTALL_FAILED_INTERNAL_ERROR, null);
            }
        } else {
            final File sourceFile = new File(mPackageURI.getPath());

            as = PackageUtil.getAppSnippet(this, appInfo, sourceFile);
            ImageView app_icon = findViewById(R.id.app_icon);
            app_icon.setImageDrawable(as.icon);

            mAppLabelView.setText(as.label);
            mFromSourceView.setText(mFromSource);
            mVersionNameView.setText(mVersionName);

            mAppInfoContainer = findViewById(R.id.app_info_container);
            mDeleteApkLayout = findViewById(R.id.delete_apk_view);
            mAutoDeleteApkTitle = findViewById(R.id.auto_delete_apk_title);

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
            });

            Button mInstallButton = findViewById(R.id.install_button);
            findViewById(R.id.space).setVisibility(View.GONE);
            mInstallButton.setVisibility(View.GONE);
            mCancelButton = findViewById(R.id.cancel_button);
            mCancelButton.setOnClickListener(
                    view -> {
                        if (mInstallingTask != null) {
                            mInstallingTask.cancel(true);
                        }

                        if (mSessionId > 0) {
                            getPackageManager().getPackageInstaller().abandonSession(mSessionId);
                            mSessionId = 0;
                        }

                        setResult(RESULT_CANCELED);
                        finish();
                    });

            requireViewById(R.id.installing).setVisibility(View.VISIBLE);

            if (savedInstanceState != null) {
                mSessionId = savedInstanceState.getInt(SESSION_ID);
                mInstallId = savedInstanceState.getInt(INSTALL_ID);

                // Reregister for result; might instantly call back if result was delivered while
                // activity was destroyed
                try {
                    InstallEventReceiver.addObserver(this, mInstallId,
                            this::launchFinishBasedOnResult);
                } catch (EventResultPersister.OutOfIdsException e) {
                    // Does not happen
                }
            } else {
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setInstallAsInstantApp(false);
                params.setReferrerUri(getIntent().getParcelableExtra(Intent.EXTRA_REFERRER));
                params.setOriginatingUri(getIntent()
                        .getParcelableExtra(Intent.EXTRA_ORIGINATING_URI));
                params.setOriginatingUid(getIntent().getIntExtra(Intent.EXTRA_ORIGINATING_UID,
                        UID_UNKNOWN));
                params.setInstallerPackageName(getIntent().getStringExtra(
                        Intent.EXTRA_INSTALLER_PACKAGE_NAME));
                params.setInstallReason(PackageManager.INSTALL_REASON_USER);

                File file = new File(mPackageURI.getPath());
                try {
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(file, 0);
                    params.setAppPackageName(pkg.packageName);
                    params.setInstallLocation(pkg.installLocation);
                    params.setSize(
                            PackageHelper.calculateInstalledSize(pkg, false, params.abiOverride));
                } catch (PackageParser.PackageParserException e) {
                    Log.e(LOG_TAG, "Cannot parse package " + file + ". Assuming defaults.");
                    Log.e(LOG_TAG,
                            "Cannot calculate installed size " + file + ". Try only apk size.");
                    params.setSize(file.length());
                } catch (IOException e) {
                    Log.e(LOG_TAG,
                            "Cannot calculate installed size " + file + ". Try only apk size.");
                    params.setSize(file.length());
                }

                try {
                    mInstallId = InstallEventReceiver
                            .addObserver(this, EventResultPersister.GENERATE_NEW_ID,
                                    this::launchFinishBasedOnResult);
                } catch (EventResultPersister.OutOfIdsException e) {
                    launchFailure(PackageManager.INSTALL_FAILED_INTERNAL_ERROR, null);
                }

                try {
                    mSessionId = getPackageManager().getPackageInstaller().createSession(params);
                } catch (IOException e) {
                    launchFailure(PackageManager.INSTALL_FAILED_INTERNAL_ERROR, null);
                }
            }

            mSessionCallback = new InstallSessionCallback();
        }
    }

    /**
     * Launch the "success" version of the final package installer dialog
     */
    private void launchSuccess() {
        Intent successIntent = new Intent(getIntent());
        successIntent.setClass(this, InstallSuccess.class);
        successIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        successIntent.putExtra("ORIGINAL_LOCATION", getIntent().getStringExtra("ORIGINAL_LOCATION"));
        successIntent.putExtra("DELETE_APK_ENABLE", getIntent().getBooleanExtra("DELETE_APK_ENABLE", false));
        successIntent.putExtra("key_fromSource", mFromSource);
        successIntent.putExtra("key_versionName", mVersionName);
        if (getIntent().getBooleanExtra("key_backInstall", false)){
            Intent intent = getIntent();
            ApplicationInfo appInfo =
                    intent.getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
                    appInfo.packageName);
            PendingIntent pendingIntent;
            String content;
            if (launchIntent != null) {
                content = getString(R.string.launch);
                pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                List<ResolveInfo> list = getPackageManager().queryIntentActivities(launchIntent,
                        0);
                if (list != null && list.size() > 0) {
                }
            }else {
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
            //TODO: 完成翻译
            if (getIntent().getBooleanExtra("DELETE_APK_ENABLE", false)) {
                Uri dataUri = Uri.parse(intent.getStringExtra("ORIGINAL_LOCATION"));
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
            File file = new File(mPackageURI.getPath());
            if (file.exists()){
                file.delete();
            }
        }else {
            startActivity(successIntent);
        }
        finish();
    }

    /**
     * Launch the "failure" version of the final package installer dialog
     *
     * @param legacyStatus  The status as used internally in the package manager.
     * @param statusMessage The status description.
     */
    private void launchFailure(int legacyStatus, String statusMessage) {
        Intent failureIntent = new Intent(getIntent());
        failureIntent.setClass(this, InstallFailed.class);
        failureIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        failureIntent.putExtra(PackageInstaller.EXTRA_LEGACY_STATUS, legacyStatus);
        failureIntent.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, statusMessage);
        failureIntent.putExtra("key_fromSource", mFromSource);
        failureIntent.putExtra("key_versionName", mVersionName);
        if (getIntent().getBooleanExtra("key_backInstall", false)){
            File file = new File(mPackageURI.getPath());
            if (file.exists()){
                file.delete();
            }
            Toast.makeText(this, setExplanationFromErrorCode(legacyStatus), Toast.LENGTH_SHORT).show();
        }else {
            startActivity(failureIntent);
        }

        finish();
    }

    private String setExplanationFromErrorCode(int statusCode) {
        Log.d(LOG_TAG, "Installation status code: " + statusCode);

        switch (statusCode) {
            case PackageManager.INSTALL_FAILED_ALREADY_EXISTS:
                return getString(R.string.install_failed_already_exists);
            case PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST:
                return getString(R.string.install_failed_bad_manifest);
            case PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME:
                return getString(R.string.install_failed_bad_package_name);
            case PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID:
                return getString(R.string.install_failed_bad_shared_user_id);
            case PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING:
                return getString(R.string.install_failed_certificate_encoding);
            case PackageManager.INSTALL_FAILED_CONFLICTING_PROVIDER:
                return String.format(getString(R.string.install_failed_conflicting_provider), as.label);
            case PackageManager.INSTALL_FAILED_CONTAINER_ERROR:
                return getString(R.string.install_failed_container_error);
            case PackageManager.INSTALL_FAILED_CPU_ABI_INCOMPATIBLE:
                return getString(R.string.install_failed_cpu_abi_incompatible);
            case PackageManager.INSTALL_FAILED_DEXOPT:
                return getString(R.string.install_failed_dexopt);
            case PackageManager.INSTALL_FAILED_DUPLICATE_PACKAGE:
                return getString(R.string.install_failed_duplicate_package);
            case PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES:
                return getString(R.string.install_failed_inconsistent_certificates);
            case PackageManager.INSTALL_FAILED_INSUFFICIENT_STORAGE:
                return getString(R.string.install_failed_insufficient_storage);
            case PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS:
                return getString(R.string.install_failed_incompatible);
            case PackageManager.INSTALL_FAILED_INTERNAL_ERROR:
                return getString(R.string.install_failed_internal_error);
            case PackageManager.INSTALL_FAILED_PACKAGE_CHANGED:
            case PackageManager.INSTALL_FAILED_UID_CHANGED:
            case PackageManager.INSTALL_FAILED_INVALID_APK:
                return getString(R.string.install_failed_invaild_apk);
            case PackageManager.INSTALL_FAILED_INVALID_INSTALL_LOCATION:
                return getString(R.string.install_failed_invaild_install_location);
            case PackageManager.INSTALL_FAILED_INVALID_URI:
                return getString(R.string.install_failed_invaild_uri);
            case PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY:
                return getString(R.string.install_failed_manifest_empty);
            case PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED:
                return getString(R.string.install_failed_manifest_malformed);
            case PackageManager.INSTALL_FAILED_MEDIA_UNAVAILABLE:
                return getString(R.string.install_failed_media_unavailable);
            case PackageManager.INSTALL_FAILED_MISSING_FEATURE:
                return getString(R.string.install_failed_missing_feature);
            case PackageManager.INSTALL_FAILED_MISSING_SHARED_LIBRARY:
                return getString(R.string.install_failed_missing_shared_library);
            case PackageManager.INSTALL_FAILED_NEWER_SDK:
                return getString(R.string.install_failed_newer_sdk);
            case PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES:
                return getString(R.string.install_failed_no_certificates);
            case PackageManager.INSTALL_FAILED_NO_SHARED_USER:
                return getString(R.string.install_failed_no_shared_user);
            case PackageManager.INSTALL_PARSE_FAILED_NOT_APK:
                return getString(R.string.install_failed_not_apk);
            case PackageManager.INSTALL_FAILED_OLDER_SDK:
                return getString(R.string.install_failed_older_sdk);
            case PackageManager.INSTALL_FAILED_REPLACE_COULDNT_DELETE:
                return getString(R.string.install_failed_replace_couldnt_delete);
            case PackageManager.INSTALL_FAILED_SHARED_USER_INCOMPATIBLE:
                return getString(R.string.install_failed_shared_user_incompatible);
            case PackageManager.INSTALL_FAILED_TEST_ONLY:
                return getString(R.string.install_failed_test_only);
            case PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION:
                return getString(R.string.install_failed_unexpected_exception);
            case PackageManager.INSTALL_FAILED_UPDATE_INCOMPATIBLE:
                return getString(R.string.install_failed_update_incompatible);
            case PackageManager.INSTALL_FAILED_USER_RESTRICTED:
                return getString(R.string.install_failed_user_restricted);
            case PackageManager.INSTALL_FAILED_VERIFICATION_FAILURE:
                return getString(R.string.install_failed_verification_failure);
            case PackageManager.INSTALL_FAILED_VERIFICATION_TIMEOUT:
                return getString(R.string.install_failed_verification_timeout);
            case PackageManager.INSTALL_FAILED_VERSION_DOWNGRADE:
                return getString(R.string.install_failed_version_downgrade);
            default:
                throw new IllegalStateException("Unexpected value: " + statusCode);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        getPackageManager().getPackageInstaller().registerSessionCallback(mSessionCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // This is the first onResume in a single life of the activity
        if (mInstallingTask == null) {
            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionInfo sessionInfo = installer.getSessionInfo(mSessionId);

            if (sessionInfo != null && !sessionInfo.isActive()) {
                mInstallingTask = new InstallingAsyncTask();
                mInstallingTask.execute();
                if (getIntent().getBooleanExtra("key_backInstall", false)){
                    moveTaskToBack(true);
                }
            } else {
                // we will receive a broadcast when the install is finished
                mCancelButton.setEnabled(false);
                setFinishOnTouchOutside(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SESSION_ID, mSessionId);
        outState.putInt(INSTALL_ID, mInstallId);
    }

    @Override
    public void onBackPressed() {
        if (mCancelButton.isEnabled()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        getPackageManager().getPackageInstaller().unregisterSessionCallback(mSessionCallback);
    }

    @Override
    protected void onDestroy() {
        if (mInstallingTask != null) {
            mInstallingTask.cancel(true);
            synchronized (mInstallingTask) {
                while (!mInstallingTask.isDone) {
                    try {
                        mInstallingTask.wait();
                    } catch (InterruptedException e) {
                        Log.i(LOG_TAG, "Interrupted while waiting for installing task to cancel",
                                e);
                    }
                }
            }
        }

        InstallEventReceiver.removeObserver(this, mInstallId);

        super.onDestroy();
    }

    /**
     * Launch the appropriate finish activity (success or failed) for the installation result.
     *
     * @param statusCode    The installation result.
     * @param legacyStatus  The installation as used internally in the package manager.
     * @param statusMessage The detailed installation result.
     */
    private void launchFinishBasedOnResult(int statusCode, int legacyStatus, String statusMessage) {
        if (statusCode == PackageInstaller.STATUS_SUCCESS) {
            launchSuccess();
        } else {
            launchFailure(legacyStatus, statusMessage);
        }
    }


    private class InstallSessionCallback extends PackageInstaller.SessionCallback {
        @Override
        public void onCreated(int sessionId) {
            // empty
        }

        @Override
        public void onBadgingChanged(int sessionId) {
            // empty
        }

        @Override
        public void onActiveChanged(int sessionId, boolean active) {
            // empty
        }

        @Override
        public void onProgressChanged(int sessionId, float progress) {
            if (sessionId == mSessionId) {
                ProgressBar progressBar = requireViewById(R.id.progress);
                progressBar.setMax(Integer.MAX_VALUE);
                progressBar.setProgress((int) (Integer.MAX_VALUE * progress));
            }
        }

        @Override
        public void onFinished(int sessionId, boolean success) {
            // empty, finish is handled by InstallResultReceiver
        }
    }

    /**
     * Send the package to the package installer and then register a event result observer that
     * will call {@link #launchFinishBasedOnResult(int, int, String)}
     */
    public final class InstallingAsyncTask extends AsyncTask<Void, Void,
            PackageInstaller.Session> {
        volatile boolean isDone;

        @Override
        protected PackageInstaller.Session doInBackground(Void... params) {
            PackageInstaller.Session session;
            try {
                session = getPackageManager().getPackageInstaller().openSession(mSessionId);
            } catch (IOException e) {
                return null;
            }

            session.setStagingProgress(0);

            try {
                File file = new File(mPackageURI.getPath());

                try (InputStream in = new FileInputStream(file)) {
                    long sizeBytes = file.length();
                    try (OutputStream out = session
                            .openWrite("PackageInstaller", 0, sizeBytes)) {
                        byte[] buffer = new byte[1024 * 1024];
                        while (true) {
                            int numRead = in.read(buffer);

                            if (numRead == -1) {
                                session.fsync(out);
                                break;
                            }

                            if (isCancelled()) {
                                session.close();
                                break;
                            }

                            out.write(buffer, 0, numRead);
                            if (sizeBytes > 0) {
                                float fraction = ((float) numRead / (float) sizeBytes);
                                session.addProgress(fraction);
                            }
                        }
                    }
                }

                return session;
            } catch (IOException | SecurityException e) {
                Log.e(LOG_TAG, "Could not write package", e);

                session.close();

                return null;
            } finally {
                synchronized (this) {
                    isDone = true;
                    notifyAll();
                }
            }
        }

        @Override
        protected void onPostExecute(PackageInstaller.Session session) {
            mCancelButton = findViewById(R.id.cancel_button);
            if (session != null) {
                Intent broadcastIntent = new Intent(BROADCAST_ACTION);
                broadcastIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                broadcastIntent.setPackage(getPackageName());
                broadcastIntent.putExtra(EventResultPersister.EXTRA_ID, mInstallId);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        InstallInstalling.this,
                        mInstallId,
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                session.commit(pendingIntent.getIntentSender());
                mCancelButton.setEnabled(false);
                setFinishOnTouchOutside(false);
            } else {
                getPackageManager().getPackageInstaller().abandonSession(mSessionId);

                if (!isCancelled()) {
                    launchFailure(PackageManager.INSTALL_FAILED_INVALID_APK, null);
                }
            }
        }
    }
}
