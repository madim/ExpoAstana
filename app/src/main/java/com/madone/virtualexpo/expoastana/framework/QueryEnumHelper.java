package com.madone.virtualexpo.expoastana.framework;

public class QueryEnumHelper {

    public static QueryEnum getQueryForId(int id, QueryEnum[] enums) {
        for (int i = 0; i < enums.length; i++) {
            if (id == enums[i].getId()) {
                return enums[i];
            }
        }
        return null;
    }
}

