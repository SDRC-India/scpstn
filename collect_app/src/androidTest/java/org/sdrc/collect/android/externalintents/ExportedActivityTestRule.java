package org.sdrc.scpstn.collect.android.externalintents;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;

class ExportedActivityTestRule<A extends Activity> extends ActivityTestRule<A> {

    ExportedActivityTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();

        ExportedActivitiesUtils.clearDirectories();
    }

}
