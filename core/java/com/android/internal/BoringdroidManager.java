/*
 * Copyright (C) 2020 The boringdroid Project
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

package com.android.internal;

import android.app.Service;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.view.IWindowManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
public class BoringdroidManager {
    public static boolean IS_SYSTEMUI_PLUGIN_ENABLED =
            SystemProperties.getBoolean("persist.sys.systemuiplugin.enabled", false);

    private static final String PACKAGE_WINDOW_BOUNDS_NAME = "package-window-bounds";
    private static final String PACKAGE_WINDOWING_MODE_NAME = "package-windowing-mode";
    private static final String PACKAGE_WINDOWING_MODE_OVERLAY_NAME = "package-windowing-mode-overlay";
    private static final List<String> DISALLOWED_LIST = new ArrayList<>();
    private static final String TAG = "BoringdroidConfig";

    static {
        DISALLOWED_LIST.add("android");
        DISALLOWED_LIST.add("com.android.systemui");
    }

    public static boolean isPCModeEnabled() {
        return SystemProperties.getBoolean("persist.sys.pcmode.enabled", true);
    }

    private static boolean isInPCModeDisallowedList(String packageName) {
        return packageName != null && DISALLOWED_LIST.contains(packageName);
    }

    private static boolean isDataSystemDirNotReady(Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        return !(userManager != null && userManager.isUserUnlockingOrUnlocked(UserHandle.myUserId()));
    }

    private static File getPackageWindowingModeFile() {
        return new File(
                Environment.getDataSystemCeDirectory(UserHandle.myUserId())
                        + File.separator + PACKAGE_WINDOWING_MODE_NAME
        );
    }

    private static File getPackageWindowingModeOverlayFile() {
        return new File(
                Environment.getDataSystemCeDirectory(UserHandle.myUserId())
                        + File.separator + PACKAGE_WINDOWING_MODE_OVERLAY_NAME
        );
    }

    private static File getPackageWindowBoundsName() {
        return new File(
                Environment.getDataSystemCeDirectory(UserHandle.myUserId())
                        + File.separator + PACKAGE_WINDOW_BOUNDS_NAME
        );
    }

    public static void savePackageWindowingMode(Context context,
                                                String packageName,
                                                @WindowConfiguration.WindowingMode int windowingMode) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling savePackageWindowingMode with package " + packageName
                    + ", and mode " + windowingMode + ", before file is ready");
            return;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getPackageWindowingModeFile(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(packageName, windowingMode).apply();
    }

    /**
     * Save package overlay windowing mode to overlay file directly.
     *
     * @param context       The {@link Context} instance to retrieve shared preferences.
     * @param packageName   The package name will be set.
     * @param windowingMode The windowing mode will be set.
     */
    public static void savePackageOverlayWindowingMode(Context context,
                                                       String packageName,
                                                       @WindowConfiguration.WindowingMode int windowingMode) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling savePackageWindowingMode with package " + packageName
                    + ", and mode " + windowingMode + ", before file is ready");
            return;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getPackageWindowingModeOverlayFile(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(packageName, windowingMode).apply();
    }

    public static void savePackageOverlayWindowingMode(String packageName,
                                                       @WindowConfiguration.WindowingMode int windowingMode) {
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Service.WINDOW_SERVICE));
        try {
            windowManager.savePackageOverlayWindowingMode(packageName, windowingMode);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IWindowManager#savePackageOverlayWindowingMode");
        }
    }

    /**
     * Get package overlay windowing mode from overlay file.
     *
     * @param context     The {@link Context} instance to retrieve shared preferences.
     * @param packageName The package name will be retrieved.
     * @return Returns the windowing mode of provided package name if existing, otherwise
     * returns {@link WindowConfiguration#WINDOWING_MODE_UNDEFINED}.
     */
    public static @WindowConfiguration.WindowingMode
    int getPackageOverlayWindowingMode(Context context, String packageName) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling getPackageWindowingMode with package " + packageName
                    + ", before file is ready");
            return WindowConfiguration.WINDOWING_MODE_UNDEFINED;
        }
        context.reloadSharedPreferences();
        SharedPreferences overlaySharedPreferences =
                context.getSharedPreferences(
                        getPackageWindowingModeOverlayFile(),
                        Context.MODE_PRIVATE
                );
        int overlayWindowingMode =
                overlaySharedPreferences.getInt(packageName, WindowConfiguration.WINDOWING_MODE_UNDEFINED);
        Slog.d(TAG, "Found overlay windowing mode " + overlayWindowingMode
                + ", for package " + packageName);
        return overlayWindowingMode;
    }

    public static @WindowConfiguration.WindowingMode
    int getPackageOverlayWindowingMode(String packageName) {
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Service.WINDOW_SERVICE));
        try {
            return windowManager.getPackageOverlayWindowingMode(packageName);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IWindowManager#getPackageOverlayWindowingMode");
            return WindowConfiguration.WINDOWING_MODE_UNDEFINED;
        }
    }

    public static @WindowConfiguration.WindowingMode
    int getPackageWindowingMode(Context context, String packageName) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling getPackageWindowingMode with package " + packageName
                    + ", before file is ready");
            return WindowConfiguration.WINDOWING_MODE_UNDEFINED;
        }
        // Okay, there is a checking chain for package windowing mode:
        // 1. If pc mode is enabled, we should set all package to undefined, and let system
        //    to calculate windowing mode based on package config.
        // 2. If package is in our defined pc disallowed list, we should set it to undefined.
        // 3. If package has windowing mode defined in overlay shared preferences, we should use
        //    whatever defined in that file. The frameworks will not change it, and leave it to
        //    other system apps or user. If you want to set specific package to specific windowing
        //    mode, just to modify it with key for package name and int value for windowing mode,
        //    based on WindowConfiguration definition. But if you set it to UNDEFINED, it will
        //    also fallback to the following config.
        // 4. If non of above, we will try to get windowing mode of package from saved shared
        //    preferences, what will be modified when user changing window mode with shortcut
        //    or decor caption bar. The default is WINDOWING_MODE_FREEFORM.
        if (!isPCModeEnabled()) {
            return WindowConfiguration.WINDOWING_MODE_UNDEFINED;
        }
        // If the package is in the multi window black list, it will run in default
        // windowing mode.
        if (isInPCModeDisallowedList(packageName)) {
            return WindowConfiguration.WINDOWING_MODE_UNDEFINED;
        }
        context.reloadSharedPreferences();
        SharedPreferences overlaySharedPreferences =
                context.getSharedPreferences(
                        getPackageWindowingModeOverlayFile(),
                        Context.MODE_PRIVATE
                );
        int overlayWindowingMode = overlaySharedPreferences.getInt(packageName, -1);
        Slog.d(TAG, "Found overlay windowing mode " + overlayWindowingMode
                + ", for package " + packageName);
        if (overlayWindowingMode != -1 && overlayWindowingMode != WindowConfiguration.WINDOWING_MODE_UNDEFINED) {
            return overlayWindowingMode;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getPackageWindowingModeFile(), Context.MODE_PRIVATE);
        // We hope the default windowing mode is freeform.
        return sharedPreferences.getInt(packageName, WindowConfiguration.WINDOWING_MODE_FREEFORM);
    }

    public static void savePackageWindowBounds(Context context, String packageName, Rect bounds) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling savePackageWindowBounds with package " + packageName
                    + ", and bounds " + bounds + ", before file is ready");
            return;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getPackageWindowBoundsName(), Context.MODE_PRIVATE);
        Rect tempBounds = new Rect(bounds);
        sharedPreferences
                .edit()
                .putInt(packageName + "-left", tempBounds.left)
                .putInt(packageName + "-top", tempBounds.top)
                .putInt(packageName + "-right", tempBounds.right)
                .putInt(packageName + "-bottom", tempBounds.bottom)
                .apply();
    }

    public static Rect getPackageWindowBounds(Context context, String packageName) {
        if (isDataSystemDirNotReady(context)) {
            Slog.e(TAG, "Calling getPackageWindowBounds with package " + packageName
                    + ", before file is ready");
            return new Rect();
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(getPackageWindowBoundsName(), Context.MODE_PRIVATE);
        return new Rect(
                sharedPreferences.getInt(packageName + "-left", 0),
                sharedPreferences.getInt(packageName + "-top", 0),
                sharedPreferences.getInt(packageName + "-right", 0),
                sharedPreferences.getInt(packageName + "-bottom", 0)
        );
    }
}