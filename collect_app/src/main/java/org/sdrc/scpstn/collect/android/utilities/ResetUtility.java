/*
 * Copyright 2017 Nafundi
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

package org.sdrc.scpstn.collect.android.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.sdrc.scpstn.collect.android.R;
import org.sdrc.scpstn.collect.android.application.Collect;
import org.sdrc.scpstn.collect.android.dao.FormsDao;
import org.sdrc.scpstn.collect.android.dao.InstancesDao;
import org.sdrc.scpstn.collect.android.database.ItemsetDbAdapter;
import org.sdrc.scpstn.collect.android.preferences.AdminPreferencesActivity;
import org.osmdroid.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.sdrc.scpstn.collect.android.preferences.PreferenceKeys.KEY_APP_LANGUAGE;

public class ResetUtility {

    private List<Integer> failedResetActions;

    public List<Integer> reset(final Context context, List<Integer> resetActions) {

        failedResetActions = new ArrayList<>();
        failedResetActions.addAll(resetActions);

        for (int action : resetActions) {
            switch (action) {
                case ResetAction.RESET_PREFERENCES:
                    resetPreferences(context);
                    break;
                case ResetAction.RESET_INSTANCES:
                    resetInstances();
                    break;
                case ResetAction.RESET_FORMS:
                    resetForms();
                    break;
                case ResetAction.RESET_LAYERS:
                    if (deleteFolderContents(Collect.OFFLINE_LAYERS)) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_LAYERS));
                    }
                    break;
                case ResetAction.RESET_CACHE:
                    if (deleteFolderContents(Collect.CACHE_PATH)) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_CACHE));
                    }
                    break;
                case ResetAction.RESET_OSM_DROID:
                    if (deleteFolderContents(Configuration.getInstance().getOsmdroidTileCache().getPath())) {
                        failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_OSM_DROID));
                    }
                    break;
            }
        }

        return failedResetActions;
    }

    private void resetPreferences(Context context) {
        /*boolean clearedDefaultPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .commit();*/
       /* SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit();
        editor.remove("splashPath").commit();*/

       boolean clearedDefaultPreferences = resetGeneralSharedpreference(context);

        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        boolean clearedAdminPreferences = context
                .getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0)
                .edit()
                .clear()
                .commit();

        boolean deletedSettingsFolderContest = !new File(Collect.SETTINGS).exists()
                || deleteFolderContents(Collect.SETTINGS);

        boolean deletedSettingsFile = !new File(Collect.ODK_ROOT + "/collect.settings").exists()
                || (new File(Collect.ODK_ROOT + "/collect.settings").delete());

        /*if (clearedDefaultPreferences && clearedAdminPreferences && deletedSettingsFolderContest && deletedSettingsFile) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_PREFERENCES));
        }*/
        if (clearedDefaultPreferences && clearedAdminPreferences && deletedSettingsFolderContest && deletedSettingsFile){
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_PREFERENCES));
        }
    }

    private void resetInstances() {
        new InstancesDao().deleteInstancesDatabase();

        if (deleteFolderContents(Collect.INSTANCES_PATH)) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_INSTANCES));
        }
    }

    private void resetForms() {
        new FormsDao().deleteFormsDatabase();

        File itemsetDbFile = new File(Collect.METADATA_PATH + File.separator + ItemsetDbAdapter.DATABASE_NAME);

        if (deleteFolderContents(Collect.FORMS_PATH) && (!itemsetDbFile.exists() || itemsetDbFile.delete())) {
            failedResetActions.remove(failedResetActions.indexOf(ResetAction.RESET_FORMS));
        }
    }

    private boolean deleteFolderContents(String path) {
        boolean result = true;
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();

            for (File f : files) {
                result = deleteRecursive(f);
            }
        }
        return result;
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        return fileOrDirectory.delete();
    }

    public static class ResetAction {
        public static final int RESET_PREFERENCES = 0;
        public static final int RESET_INSTANCES = 1;
        public static final int RESET_FORMS = 2;
        public static final int RESET_LAYERS = 3;
        public static final int RESET_CACHE = 4;
        public static final int RESET_OSM_DROID = 5;
    }

    public boolean resetGeneralSharedpreference (Context context){

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit();

        if (!editor.remove("splashPath").commit()){
            return false;
        }if (!editor.remove("showSplash").commit()){
            return false;
        }if (!editor.remove("formChooserListSortingOrder").commit()){
            return false;
        }if (!editor.remove("instance_sync").commit()){
            return false;
        }if (!editor.remove("delete_send").commit()){
            return false;
        }if (!editor.remove("map_basemap_behavior").commit()){
            return false;
        }if (!editor.remove("high_resolution").commit()){
            return false;
        }if (!editor.remove("metadata_migrated").commit()){
            return false;
        }if (!editor.remove("map_sdk_behavior").commit()){
            return false;
        }if (!editor.remove("default_completed").commit()){
            return false;
        }if (!editor.remove("font_size").commit()){
            return false;
        }if (!editor.remove("app_language").commit()){
            return false;
        }if (!editor.remove("navigation").commit()){
            return false;
        }if (!editor.remove("constraint_behavior").commit()){
            return false;
        }if (!editor.remove("autosend").commit()){
            return false;
        }if (!editor.remove("analytics").commit()){
            return false;
        }if (!editor.remove("metadata_phonenumber").commit()){
            return false;
        }if (!editor.remove("metadata_email").commit()){
            return false;
        }if (!editor.remove("metadata_username").commit()){
            return false;
        }

        SharedPreferences.Editor edit = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        edit.putString(KEY_APP_LANGUAGE, "en");
        edit.apply();
        LocaleHelper localeHelper = new LocaleHelper();
        localeHelper.updateLocale(context);

        return true;
    }
}
