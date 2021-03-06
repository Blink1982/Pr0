package com.pr0gramm.app.services;

import android.annotation.SuppressLint;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.crashlytics.android.answers.RatingEvent;
import com.crashlytics.android.answers.SearchEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.pr0gramm.app.Settings;
import com.pr0gramm.app.feed.FeedType;
import com.pr0gramm.app.feed.Vote;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Action1;

/**
 * Tracking using crashlytics answers. Obviously this is anonymous and you can
 * opt-out in the applications settings.
 */
public final class Track {
    private static final Logger logger = LoggerFactory.getLogger("Track");

    private Track() {
    }

    public static void loginSuccessful() {
        track(answers -> answers.logLogin(new LoginEvent().putSuccess(true)));
    }

    public static void loginFailed() {
        track(answers -> answers.logLogin(new LoginEvent().putSuccess(false)));
    }

    public static void logout() {
        track(new CustomEvent("Logout"));
    }

    public static void search(String query) {
        track(answers -> answers.logSearch(new SearchEvent().putQuery(query)));
    }

    public static void writeComment() {
        track(new CustomEvent("WriteComment"));
    }

    public static void writeMessage() {
        track(new CustomEvent("WriteMessage"));
    }

    public static void searchImage() {
        track(new CustomEvent("SearchImage"));
    }

    public static void share(String type) {
        track(answers -> answers.logShare(new ShareEvent().putMethod(type)));
    }

    public static void votePost(Vote vote) {
        track(answers -> answers.logRating(new RatingEvent()
                .putContentType("post")
                .putRating(vote.getVoteValue())));
    }

    public static void voteTag(Vote vote) {
        track(answers -> answers.logRating(new RatingEvent()
                .putContentType("tag")
                .putRating(vote.getVoteValue())));
    }

    public static void voteComment(Vote vote) {
        track(answers -> answers.logRating(new RatingEvent()
                .putContentType("comment")
                .putRating(vote.getVoteValue())));
    }

    public static void upload(long size) {
        long categoryStart = size / (512 * 1024) * 512;

        @SuppressLint("DefaultLocale")
        String sizeCategory = String.format("%d-%d kb", categoryStart, categoryStart + 512);
        track(answers -> answers.logCustom(new CustomEvent("Upload")
                .putCustomAttribute("size", sizeCategory)));
    }

    public static void download() {
        track(new CustomEvent("Download"));
    }

    public static void vpxWouldWork(boolean okay) {
        String valueStr = String.valueOf(okay);
        track(new CustomEvent("VpxWouldWork")
                .putCustomAttribute("wouldWork", valueStr));
    }

    public static void statistics(Settings settings, boolean signedIn) {
        track(new CustomEvent("Settings")
                .putCustomAttribute("beta", String.valueOf(settings.useBetaChannel()))
                .putCustomAttribute("signed in", String.valueOf(signedIn))
                .putCustomAttribute("gif2webm", String.valueOf(settings.convertGifToWebm()))
                .putCustomAttribute("notifications", String.valueOf(settings.showNotifications()))
                .putCustomAttribute("mark images", settings.seenIndicatorStyle().name())
                .putCustomAttribute("https", String.valueOf(settings.useHttps()))
                .putCustomAttribute("theme", settings.themeName().toLowerCase())
                .putCustomAttribute("bestof threshold", String.valueOf(settings.bestOfBenisThreshold()))
                .putCustomAttribute("quick preview", String.valueOf(settings.enableQuickPeek()))
                .putCustomAttribute("volume navigation", String.valueOf(settings.volumeNavigation()))
                .putCustomAttribute("hide tag vote buttons", String.valueOf(settings.hideTagVoteButtons()))
                .putCustomAttribute("incognito browser", String.valueOf(settings.useIncognitoBrowser())));
    }

    public static void bookmarks(int size) {
        track(new CustomEvent("Bookmarks loaded")
                .putCustomAttribute("bookmarks", String.valueOf(size)));
    }

    public static void notificationShown() {
        track(new CustomEvent("Notification shown"));
    }

    public static void notificationClosed(String method) {
        track(new CustomEvent("Notification closed").putCustomAttribute("method", method));
    }

    public static void requestFeed(FeedType feedType) {
        track(new CustomEvent("Load feed")
                .putCustomAttribute("feed type", feedType.name()));
    }

    public static void preloadCurrentFeed(int size) {
        int hour = Instant.now().toDateTime(DateTimeZone.UTC).getHourOfDay();

        track(new CustomEvent("Preload current feed")
                .putCustomAttribute("hour", String.valueOf(hour))
                .putCustomAttribute("itemCount", size));
    }

    public static void inviteSent() {
        track(new CustomEvent("Invite sent"));
    }

    public static void commentFaved() {
        track(new CustomEvent("Comment faved"));
    }

    public static void listFavedComments() {
        track(new CustomEvent("Faved comments listed"));
    }

    public static void quickPeek() {
        track(new CustomEvent("Quick peek used"));
    }

    public static void collapseComments(String action) {
        track(new CustomEvent("Collapse Comment").putCustomAttribute("action", action));
    }

    public static void muted(boolean mute) {
        track(new CustomEvent("Muted").putCustomAttribute("action", mute ? "MUTED" : "UNMUTED"));
    }

    private static void track(CustomEvent customEvent) {
        track(answers -> answers.logCustom(customEvent));
    }

    /**
     * Only do the tracking if 'answers' is active.
     */
    private static void track(Action1<Answers> action) {
        try {
            Answers instance = Answers.getInstance();
            if (instance != null) {
                action.call(instance);
            } else {
                logger.info("Would track an event now");
            }
        } catch (IllegalStateException error) {
            logger.warn("Tried to log without initializing crashlytics");
        }
    }
}
