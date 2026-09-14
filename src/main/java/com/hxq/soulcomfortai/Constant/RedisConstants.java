package com.hxq.soulcomfortai.Constant;

public class RedisConstants {

    public static final long DEFAULT_TTL_SECONDS = 90 * 24 * 60 * 60;

    private static final String NS = "sc:";

    public static final String SEQ_USER_ID = NS + "seq:user:id";
    public static final String SEQ_DIARY_ID = NS + "seq:diary:id";
    public static final String SEQ_CONV_ID = NS + "seq:conv:id";
    public static final String SEQ_EMOTION_ID = NS + "seq:emotion:id";
    public static final String SEQ_CHECKIN_ID = NS + "seq:checkin:id";

    public static final String USER_ID_PREFIX = "u_";
    public static final String DIARY_ID_PREFIX = "d_";
    public static final String CONV_ID_PREFIX = "c_";
    public static final String EMOTION_ID_PREFIX = "e_";
    public static final String CHECKIN_ID_PREFIX = "ck_";

    public static String userKey(String userId) {
        return NS + "user:" + userId;
    }

    public static String usernameIndexKey(String username) {
        return NS + "user:index:username:" + username;
    }

    public static String diaryKey(String diaryId) {
        return NS + "diary:" + diaryId;
    }

    public static String userDiariesKey(String userId) {
        return NS + "diary:index:user:" + userId;
    }

    public static String convKey(String convId) {
        return NS + "conv:" + convId;
    }

    public static String convMessagesKey(String convId) {
        return NS + "conv:" + convId + ":msg";
    }

    public static String userConvsKey(String userId) {
        return NS + "conv:index:user:" + userId;
    }

    public static String userConvsByTagKey(String userId, String tag) {
        return NS + "conv:index:user:" + userId + ":tag:" + tag;
    }

    public static String convTagsKey() {
        return NS + "conv:tags";
    }

    public static String emotionKey(String emotionId) {
        return NS + "emotion:" + emotionId;
    }

    public static String userEmotionDailyKey(String userId, String date) {
        return NS + "emotion:daily:" + userId + ":" + date;
    }

    public static String userEmotionSummaryKey(String userId) {
        return NS + "emotion:summary:" + userId;
    }

    public static String suggestedQuestionsKey(String convId, String lastMessageId) {
        return NS + "cache:suggested:" + convId + ":" + lastMessageId;
    }

    public static String dailyGreetingKey(String userId, String date) {
        return NS + "cache:daily:greeting:" + userId + ":" + date;
    }

    public static String insightKey(String userId, int days) {
        return NS + "cache:insight:" + userId + ":" + days;
    }

    public static String loginFailKey(String username) {
        return NS + "ratelimit:login:fail:" + username;
    }

    public static String loginLockKey(String username) {
        return NS + "ratelimit:login:lock:" + username;
    }

    public static String avatarKey(String userId) {
        return NS + "user:avatar:" + userId;
    }

    public static String checkinKey(String checkinId) {
        return NS + "checkin:" + checkinId;
    }

    public static String userCheckinDailyKey(String userId, String date) {
        return NS + "checkin:daily:" + userId + ":" + date;
    }

    public static String userCheckinListKey(String userId) {
        return NS + "checkin:index:user:" + userId;
    }
}