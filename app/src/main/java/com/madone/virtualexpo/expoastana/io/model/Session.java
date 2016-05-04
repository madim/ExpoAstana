package com.madone.virtualexpo.expoastana.io.model;

import java.util.Random;

public class Session {
    public String id;
    public String description;
    public String title;
    public String[] tags;
    public String startTimestamp;
    public String endTimestamp;
    public String hashtag;
    public String room;
    public String photoUrl;
    public String mainTag;
    public String color;
    public int groupingOrder;

    public class RelatedContent {
        public String id;
        public String name;

        @Override
        public String toString() {
            return "RelatedContent{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public String getImportHashCode() {
        return (new Random()).nextLong()+"";
    }

    public String makeTagsList() {
        int i;
        if (tags.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(tags[0]);
        for (i = 1; i < tags.length; i++) {
            sb.append(",").append(tags[i]);
        }
        return sb.toString();
    }

    public boolean hasTag(String tag) {
        for (String myTag : tags) {
            if (myTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

}