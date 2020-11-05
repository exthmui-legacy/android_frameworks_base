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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.service.trust.TrustAgentService;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;

import java.io.File;

/**
 * Installation failed: Return status code to the caller or display failure UI to user
 */
public class InstallFailed extends Activity {
    private static final String LOG_TAG = InstallFailed.class.getSimpleName();

    /**
     * Label of the app that failed to install
     */
    private CharSequence mLabel;

    private PackageUtil.AppSnippet as;

    private String mFromSource;
    private String mVersionName;

    private TextView mAppLabelView;
    private TextView mFromSourceView;
    private TextView mVersionNameView;

    private CardView mDeleteApkLayout;
    private CardView mAppInfoContainer;
    private TextView mAutoDeleteApkTitle;

    /**
     * Unhide the appropriate label for the statusCode.
     *
     * @param statusCode The status code from the package installer.
     */
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int statusCode = getIntent().getIntExtra(PackageInstaller.EXTRA_LEGACY_STATUS,
                PackageInstaller.STATUS_FAILURE);

        setContentView(R.layout.install_main);
        mFromSource = getIntent().getStringExtra("key_fromSource");
        mVersionName = getIntent().getStringExtra("key_versionName");

        mAppLabelView = findViewById(R.id.app_name);
        mFromSourceView = findViewById(R.id.from_source);
        mVersionNameView = findViewById(R.id.app_versionName);

        mAppInfoContainer = findViewById(R.id.app_info_container);
        mDeleteApkLayout = findViewById(R.id.delete_apk_view);
        mAutoDeleteApkTitle = findViewById(R.id.auto_delete_apk_title);

        if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
            int legacyStatus = getIntent().getIntExtra(PackageInstaller.EXTRA_LEGACY_STATUS,
                    PackageManager.INSTALL_FAILED_INTERNAL_ERROR);

            // Return result if requested
            Intent result = new Intent();
            result.putExtra(Intent.EXTRA_INSTALL_RESULT, legacyStatus);
            setResult(Activity.RESULT_FIRST_USER, result);
            finish();
        } else {
            Intent intent = getIntent();
            ApplicationInfo appInfo = intent
                    .getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
            Uri packageURI = intent.getData();

            // Set header icon and title
            PackageManager pm = getPackageManager();
            CharSequence versionName = null;
            try {
                versionName = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_CONFIGURATIONS).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if ("package".equals(packageURI.getScheme())) {
                as = new PackageUtil.AppSnippet(pm.getApplicationLabel(appInfo), versionName,
                        pm.getApplicationIcon(appInfo));
            } else {
                final File sourceFile = new File(packageURI.getPath());
                as = PackageUtil.getAppSnippet(this, appInfo, sourceFile);
            }

            // Store label for dialog
            mLabel = as.label;

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
            });

            Button mInstallButton = findViewById(R.id.install_button);
            mInstallButton.setVisibility(View.GONE);
            findViewById(R.id.space).setVisibility(View.GONE);
            Button mCancelButton = findViewById(R.id.cancel_button);
            TextView mInstallResult = findViewById(R.id.install_result);
            LinearLayout mInstallResultContainer = findViewById(R.id.install_result_container);
            ImageView mInstallResultImage = findViewById(R.id.install_result_image);
            mInstallResultImage.setImageResource(R.drawable.ic_install_error);
            mInstallResultContainer.setVisibility(View.VISIBLE);
            mInstallResult.setText(getString(R.string.install_failed));
            mCancelButton.setText(R.string.install_failed_exit);
            mCancelButton.setOnClickListener(view -> finish());

            // Show out of space dialog if needed
            if (statusCode == PackageInstaller.STATUS_FAILURE_STORAGE) {
                (new OutOfSpaceDialog()).show(getFragmentManager(), "outofspace");
            }

            String statusMessage = setExplanationFromErrorCode(statusCode);
            String mErrorCode = String.format(getString(R.string.install_status_code), statusCode);
            // Get status messages
            TextView mStatusMessage = requireViewById(R.id.install_status);
            mStatusMessage.setText(statusMessage);
            mStatusMessage.setVisibility(View.VISIBLE);

            TextView mStatusErrorCode = requireViewById(R.id.install_error_code);
            mStatusErrorCode.setText(mErrorCode);
            mStatusErrorCode.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dialog shown when we ran out of space during installation. This contains a link to the
     * "manage applications" settings page.
     */
    public static class OutOfSpaceDialog extends DialogFragment {
        private InstallFailed mActivity;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            mActivity = (InstallFailed) context;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.out_of_space_dlg_title)
                    .setMessage(getString(R.string.out_of_space_dlg_text, mActivity.mLabel))
                    .setPositiveButton(R.string.manage_applications, (dialog, which) -> {
                        // launch manage applications
                        Intent intent = new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE");
                        startActivity(intent);
                        mActivity.finish();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> mActivity.finish())
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            mActivity.finish();
        }
    }
}
