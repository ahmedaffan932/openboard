/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.dslul.openboard.inputmethod.latin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The keyboard's settings live in the default SharedPreferences of device-protected storage, so
 * the keyboard can read them before the user unlocks the phone.
 *
 * <p>Every caller gets the same process-wide instance. That matters: the keyboard ({@code
 * Settings}) registers its change listener on the instance it holds, and the settings screens
 * write through Android's own per-file cache. The old version called {@code
 * moveSharedPreferencesFrom} whenever the settings were empty, with the source and the target
 * being the same file (the app sets {@code defaultToDeviceProtectedStorage}). If that file existed
 * but was empty, Android copied it onto itself, deleted it and evicted its cache, which left the
 * keyboard holding a detached instance: changes made in the settings screen never reached it until
 * the keyboard service was re-created (e.g. by the mic key switching to voice typing and back).
 */
public final class DeviceProtectedUtils {

    static final String TAG = DeviceProtectedUtils.class.getSimpleName();

    private static volatile SharedPreferences sPrefs;

    public static SharedPreferences getSharedPreferences(final Context context) {
        final SharedPreferences cached = sPrefs;
        if (cached != null) {
            return cached;
        }
        synchronized (DeviceProtectedUtils.class) {
            if (sPrefs == null) {
                sPrefs = openSharedPreferences(context);
            }
            return sPrefs;
        }
    }

    private static SharedPreferences openSharedPreferences(final Context context) {
        if (context.isDeviceProtectedStorage()) {
            // Always the case in this app: it sets defaultToDeviceProtectedStorage, so its contexts
            // already point at device-protected storage and there is nothing to migrate. Calling
            // moveSharedPreferencesFrom here would move the file onto itself (see above).
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
        final Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(deviceProtectedContext);
        if (prefs.getAll().isEmpty()) {
            Log.i(TAG, "Device-protected settings are empty, moving them from credential storage");
            if (deviceProtectedContext.moveSharedPreferencesFrom(context,
                    PreferenceManager.getDefaultSharedPreferencesName(context))) {
                // The move evicts Android's cached instance for this file; open it again.
                return PreferenceManager.getDefaultSharedPreferences(deviceProtectedContext);
            }
        }
        return prefs;
    }

    private DeviceProtectedUtils() {
        // This utility class is not publicly instantiable.
    }
}
