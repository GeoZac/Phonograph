package com.kabouzeid.gramophone;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.bugsnag.android.Bugsnag;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

    private static App app;

    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            Bugsnag.start(this);
        }
        app = this;

        context = getApplicationContext();

        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
                    .primaryColorRes(R.color.dark_primary)
                    .accentColorRes(R.color.md_pink_A400)
                    .coloredNavigationBar(true)
                    .commit();
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }
    }

    public static App getInstance() {
        return app;
    }

    public static Context getStaticContext() {
        return context;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
