/*
 * Copyright (C) 2020 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util;

import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DeviceHookPropsUtils {

    private static final String TAG = DeviceHookPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Map<String, Object> propsToChangeGoogle;
    private static final Map<String, Object> propsToChangePixel3XLGoogle;
    private static final Map<String, Object> propsToChangeMeizu;

    private static final String[] packagesToChangeGoogle = {
            "com.breel.wallpapers20",
            "com.google.android.apps.customization.pixel",
            "com.google.android.apps.fitness",
            "com.google.android.apps.photos",
            "com.google.android.apps.recorder",
            "com.google.android.apps.subscriptions.red",
            "com.google.android.apps.tachyon",
            "com.google.android.apps.turboadapter",
            "com.google.android.apps.wallpaper.pixel",
            "com.google.android.as",
            "com.google.android.dialer",
            "com.google.android.gms.location.history",
            "com.google.android.inputmethod.latin",
            "com.google.android.soundpicker",
            "com.google.pixel.dynamicwallpapers",
            "com.google.pixel.livewallpaper",
            "com.google.android.apps.safetyhub",
            "com.google.android.apps.turbo",
            "com.google.android.apps.wallpaper",
            "com.google.android.apps.maps",
            "com.google.android.gms",
            "com.google.android.apps.nexuslauncher"
    };

    private static final String[] packagesToChangePixel3XLGoogle = {
            "com.google.android.googlequicksearchbox"
    };

    private static final String[] packagesToChangeMeizu = {
            "com.netease.cloudmusic",
            "com.tencent.qqmusic",
            "com.kugou.android",
            "cmccwm.mobilemusic",
            "cn.kuwo.player",
            "com.meizu.media.music"
    };

    static {
        propsToChangeGoogle = new HashMap<>();
        propsToChangeGoogle.put("BRAND", "google");
        propsToChangeGoogle.put("MANUFACTURER", "Google");
        propsToChangeGoogle.put("DEVICE", "redfin");
        propsToChangeGoogle.put("PRODUCT", "redfin");
        propsToChangeGoogle.put("MODEL", "Pixel 5");
        propsToChangeGoogle.put("FINGERPRINT", "google/redfin/redfin:11/RQ2A.210505.003/7255357:user/release-keys");
        propsToChangePixel3XLGoogle = new HashMap<>();
        propsToChangePixel3XLGoogle.put("BRAND", "google");
        propsToChangePixel3XLGoogle.put("MANUFACTURER", "Google");
        propsToChangePixel3XLGoogle.put("DEVICE", "crosshatch");
        propsToChangePixel3XLGoogle.put("PRODUCT", "crosshatch");
        propsToChangePixel3XLGoogle.put("MODEL", "Pixel 3 XL");
        propsToChangePixel3XLGoogle.put("FINGERPRINT", "google/crosshatch/crosshatch:11/RQ2A.210505.002/7246365:user/release-keys");
        propsToChangeMeizu = new HashMap<>();
        propsToChangeMeizu.put("BRAND", "meizu");
        propsToChangeMeizu.put("MANUFACTURER", "meizu");
        propsToChangeMeizu.put("DEVICE", "meizu18");
        propsToChangeMeizu.put("PRODUCT", "meizu_18_CN");
        propsToChangeMeizu.put("MODEL", "MEIZU 18");
        propsToChangeMeizu.put("FINGERPRINT", "meizu/meizu_18_CN/meizu18:11/RKQ1.201105.002/1607588916:user/release-keys");
    }

    public static void setProps(String packageName) {
        if (packageName == null){
            return;
        }
        if (Arrays.asList(packagesToChangeGoogle).contains(packageName)){
            if (DEBUG){
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChangeGoogle.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
        if (Arrays.asList(packagesToChangePixel3XLGoogle).contains(packageName)){
            if (DEBUG){
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChangePixel3XLGoogle.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
        if (Arrays.asList(packagesToChangeMeizu).contains(packageName)){
            if (DEBUG){
                Log.d(TAG, "Defining props for: " + packageName);
            }
            for (Map.Entry<String, Object> prop : propsToChangeMeizu.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                setPropValue(key, value);
            }
        }
        // Set proper indexing fingerprint
        /*if (packageName.equals("com.google.android.settings.intelligence")){
            setPropValue("FINGERPRINT", Build.DATE);
        }*/
    }

    private static void setPropValue(String key, Object value){
        try {
            if (DEBUG){
                Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            }
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }
}