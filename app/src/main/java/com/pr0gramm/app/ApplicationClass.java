package com.pr0gramm.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.f2prateek.dart.Dart;
import com.facebook.stetho.Stetho;
import com.orm.SugarApp;
import com.pr0gramm.app.services.SingleShotService;
import com.pr0gramm.app.services.ThemeHelper;
import com.pr0gramm.app.ui.ActivityErrorHandler;
import com.pr0gramm.app.util.CrashlyticsLogHandler;
import com.pr0gramm.app.util.HandlerThreadScheduler;
import com.pr0gramm.app.util.Lazy;

import net.danlew.android.joda.JodaTimeAndroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import pl.brightinventions.slf4android.LoggerConfiguration;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;

import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.setGlobalErrorDialogHandler;

/**
 * Global application class for pr0gramm app.
 */
public class ApplicationClass extends SugarApp {
    private static final Logger logger = LoggerFactory.getLogger("Pr0grammApplication");

    final Lazy<AppComponent> appComponent = Lazy.of(() -> DaggerAppComponent.builder()
            .appModule(new AppModule(this))
            .httpModule(new HttpModule())
            .build());

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);

        Settings settings = Settings.of(this);
        if (BuildConfig.DEBUG) {
            logger.info("This is a development version.");
            StrictMode.enableDefaults();
            ButterKnife.setDebug(true);
            Dart.setDebug(true);

        } else {
            if (settings.analyticsEnabled()) {
                logger.info("Initialize Fabric");
                Fabric.with(this, new Crashlytics());
                Fabric.with(this, new Answers());

                LoggerConfiguration.configuration()
                        .removeRootLogcatHandler()
                        .addHandlerToRootLogger(new CrashlyticsLogHandler());

            } else {
                // just disable logging
                LoggerConfiguration.configuration()
                        .removeRootLogcatHandler();
            }
        }

        // initialize this to show errors always in the context of the current activity.
        setGlobalErrorDialogHandler(new ActivityErrorHandler(this));

        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return HandlerThreadScheduler.INSTANCE;
            }
        });

        Dagger.initEagerSingletons(this);

        if (BuildConfig.DEBUG) {
            logger.info("Setup stetho");
            Stetho.initializeWithDefaults(this);

            // Setup network interceptor for okhttp.
            // OkHttpClient okHttpClient = appComponent.get().okHttpClient();
            // okHttpClient.networkInterceptors().add(new StethoInterceptor());
        }

        SingleShotService singleShotService = appComponent.get().singleShotService();
        if (singleShotService.isFirstTime("migrate.MpegDecoderToVPX")) {
            if (settings.forceMpegDecoder()) {
                logger.info("Switch to normal software decoder");

                settings.edit()
                        .putBoolean("pref_force_mpeg_decoder", false)
                        .putBoolean("pref_use_software_decoder", true)
                        .apply();
            }
        }

        // get the correct theme for the app!
        ThemeHelper.updateTheme(this);
    }

    /**
     * Opens the community in the playstore.
     */
    public static void openCommunityWebpage(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/apps/testing/io.github.mopsalarm.pr0gramm.gp2"));

        activity.startActivity(intent);
    }

    public static void openDownloadFreeVersion(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://app.pr0gramm.com/#download"));

        activity.startActivity(intent);
    }

    public static ApplicationClass get(Context context) {
        return (ApplicationClass) context.getApplicationContext();
    }
}