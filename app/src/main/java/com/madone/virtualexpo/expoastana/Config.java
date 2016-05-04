package com.madone.virtualexpo.expoastana;

import com.madone.virtualexpo.expoastana.util.ParserUtils;

import java.util.HashMap;
import java.util.Map;

public class Config {

    public static final long[][] CONFERENCE_DAYS = new long[][]{
            {ParserUtils.parseTime(BuildConfigs.CONFERENCE_DAY1_START),
                    ParserUtils.parseTime(BuildConfigs.CONFERENCE_DAY1_END)},
            {ParserUtils.parseTime(BuildConfigs.CONFERENCE_DAY2_START),
                    ParserUtils.parseTime(BuildConfigs.CONFERENCE_DAY2_END)},
    };
    public static final long CONFERENCE_END_MILLIS = CONFERENCE_DAYS[CONFERENCE_DAYS.length - 1][1];
    public interface Tags {

        public static final String SESSIONS = "TYPE_SESSIONS";

        public static final String SESSION_GROUPING_TAG_CATEGORY = "TYPE";

        public static final String CATEGORY_THEME = "THEME";
        public static final String CATEGORY_TOPIC = "TOPIC";
        public static final String CATEGORY_TYPE = "TYPE";

        public static final Map<String, Integer> CATEGORY_DISPLAY_ORDERS
                = new HashMap<String, Integer>();

        public static final String SPECIAL_KEYNOTE = "FLAG_KEYNOTE";

        public static final String[] EXPLORE_CATEGORIES =
                {CATEGORY_THEME, CATEGORY_TOPIC, CATEGORY_TYPE};

        public static final int[] EXPLORE_CATEGORY_ALL_STRING = {
                R.string.all_themes, R.string.all_topics, R.string.all_types
        };

        public static final int[] EXPLORE_CATEGORY_TITLE = {
                R.string.themes, R.string.topics, R.string.types
        };
    }

    static {
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_THEME, 0);
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_TOPIC, 1);
        Tags.CATEGORY_DISPLAY_ORDERS.put(Tags.CATEGORY_TYPE, 2);
    }

}