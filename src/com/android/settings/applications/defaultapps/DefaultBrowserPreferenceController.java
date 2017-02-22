/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import java.util.List;

public class DefaultBrowserPreferenceController extends DefaultAppPreferenceController {

    static final Intent BROWSE_PROBE = new Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("http:"));

    public DefaultBrowserPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        final List<ResolveInfo> candidates = getCandidates();
        return candidates != null && !candidates.isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return "default_browser";
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final DefaultAppInfo defaultApp = getDefaultAppInfo();
        final CharSequence defaultAppLabel = defaultApp != null
                ? defaultApp.loadLabel(mPackageManager.getPackageManager()) : null;
        if (TextUtils.isEmpty(defaultAppLabel)) {
            final String onlyAppLabel = getOnlyAppLabel();
            if (!TextUtils.isEmpty(onlyAppLabel)) {
                preference.setSummary(onlyAppLabel);
            }
        }
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        try {
            return new DefaultAppInfo(mPackageManager.getPackageManager().getApplicationInfo(
                    mPackageManager.getDefaultBrowserPackageNameAsUser(mUserId), 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<ResolveInfo> getCandidates() {
        return mPackageManager.queryIntentActivitiesAsUser(BROWSE_PROBE, PackageManager.MATCH_ALL,
                mUserId);
    }

    private String getOnlyAppLabel() {
        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        final List<ResolveInfo> list = getCandidates();
        if (list != null && list.size() == 1) {
            return list.get(0).loadLabel(mPackageManager.getPackageManager()).toString();
        }
        return null;
    }

    /**
     * Whether or not the pkg contains browser capability
     */
    public static boolean hasBrowserPreference(String pkg, Context context) {
        final Intent intent = new Intent(BROWSE_PROBE);
        intent.setPackage(pkg);
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    /**
     * Whether or not the pkg is the default browser
     */
    public boolean isBrowserDefault(String pkg, int userId) {
        String defaultPackage = mPackageManager.getDefaultBrowserPackageNameAsUser(userId);
        if (defaultPackage != null) {
            return defaultPackage.equals(pkg);
        }

        final List<ResolveInfo> list = mPackageManager.queryIntentActivitiesAsUser(BROWSE_PROBE,
                PackageManager.MATCH_ALL, userId);
        // There is only 1 app, it must be the default browser.
        return list != null && list.size() == 1;
    }
}
