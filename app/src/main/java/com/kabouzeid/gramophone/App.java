package com.kabouzeid.gramophone;

import android.app.Application;
import android.os.Build;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager;
import io.fabric.sdk.android.Fabric;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {
    public static final String TAG = App.class.getSimpleName();

    public static final String GOOGLE_PLAY_LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjMeADN5Ffnt/ml5SYxNPCn8kGcOYGpHEfNSCts99vVxqmCn6C01E94c17j7rUK2aeHur5uxphZylzopPlQ8P8l1fqty0GPUNRSo18FCJzfGH8HZAwZYOcnRFPaXdaq3InyFJhBiODh2oeAcVK/idH6QraQ4r9HIlzigAg6lgwzxl2wJKDh7X/GMdDntCyzDh8xDQ0wIawFgvgojHwqh2Ci8Gnq6EYRwPA9yHiIIksT8Q30QyM5ewl5QcnWepsls7enNqeHarhpmSibRUDgCsxHoOpny7SyuvZvUI3wuLckDR0ds9hrt614scHHqDOBp/qWCZiAgOPVAEQcURbV09qQIDAQAB";
    public static final String PRO_VERSION_PRODUCT_ID = "pro_version";

    private static App app;

    private BillingProcessor billingProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        app = this;

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

        // automatically restores purchases
        billingProcessor = new BillingProcessor(this, App.GOOGLE_PLAY_LICENSE_KEY, new BillingProcessor.IBillingHandler() {
            @Override
            public void onProductPurchased(String productId, TransactionDetails details) {
            }

            @Override
            public void onPurchaseHistoryRestored() {
//                Toast.makeText(App.this, R.string.restored_previous_purchase_please_restart, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBillingError(int errorCode, Throwable error) {
            }

            @Override
            public void onBillingInitialized() {
            }
        });
    }

    public static boolean isProVersion() {
        return true;
    }

    public static App getInstance() {
        return app;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        billingProcessor.release();
    }
}
