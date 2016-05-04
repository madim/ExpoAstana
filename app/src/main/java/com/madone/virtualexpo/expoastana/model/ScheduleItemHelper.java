package com.madone.virtualexpo.expoastana.model;

import java.util.*;

public class ScheduleItemHelper {

    private static final long FREE_BLOCK_MINIMUM_LENGTH = 10 * 60 * 1000; // 10 minutes
    public static final long ALLOWED_OVERLAP = 5 * 60 * 1000; // 5 minutes

    static public ArrayList<ScheduleItem> processItems(ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {

        moveMutables(mutableItems, immutableItems);

        markConflicting(immutableItems);

        ArrayList<ScheduleItem> result = new ArrayList<ScheduleItem>();
        result.addAll(immutableItems);
        result.addAll(mutableItems);

        Collections.sort(result, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem lhs, ScheduleItem rhs) {
                return lhs.startTime < rhs.startTime ? -1 : 1;
            }
        });

        return result;
    }

    static protected void markConflicting(ArrayList<ScheduleItem> items) {
        for (int i=0; i<items.size(); i++) {
            ScheduleItem item = items.get(i);
            if (item.type == ScheduleItem.SESSION) for (int j=i+1; j<items.size(); j++) {
                ScheduleItem other = items.get(j);
                if (item.type == ScheduleItem.SESSION) {
                    if (intersect(other, item, true)) {
                        other.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS;
                        item.flags |= ScheduleItem.FLAG_CONFLICTS_WITH_NEXT;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    static protected void moveMutables(ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {
        Iterator<ScheduleItem> immutableIt = immutableItems.iterator();

        while (immutableIt.hasNext()) {
            ScheduleItem immutableItem = immutableIt.next();
            if (immutableItem.type == ScheduleItem.BREAK) {
                continue;
            }
            ListIterator<ScheduleItem> mutableIt = mutableItems.listIterator();
            while (mutableIt.hasNext()) {
                ScheduleItem mutableItem = mutableIt.next();
                ScheduleItem split = null;

                if (intersect(immutableItem, mutableItem, true)) {
                    if (isContainedInto(mutableItem, immutableItem)) {
                        mutableIt.remove();
                        continue;
                    } else if (isContainedInto(immutableItem, mutableItem)) {
                        if (isIntervalLongEnough(immutableItem.endTime, mutableItem.endTime)) {
                            split = (ScheduleItem) mutableItem.clone();
                            split.startTime = immutableItem.endTime;
                        }
                        mutableItem.endTime = immutableItem.startTime;
                    } else if (mutableItem.startTime < immutableItem.endTime) {
                        mutableItem.startTime = immutableItem.endTime;
                    } else if (mutableItem.endTime > immutableItem.startTime) {
                        mutableItem.endTime = immutableItem.startTime;
                    }

                    if (!isIntervalLongEnough(mutableItem.startTime, mutableItem.endTime)) {
                        mutableIt.remove();
                    }
                    if (split != null) {
                        mutableIt.add(split);
                    }
                }
            }
        }

    }

    static private boolean isIntervalLongEnough(long start, long end) {
        return ( end - start ) >= FREE_BLOCK_MINIMUM_LENGTH;
    }
    static private boolean intersect(ScheduleItem block1, ScheduleItem block2, boolean useOverlap) {
        return block2.endTime > ( block1.startTime + ( useOverlap ? ALLOWED_OVERLAP : 0 ) )
                && ( block2.startTime + ( useOverlap ?ALLOWED_OVERLAP : 0 ) ) < block1.endTime;
    }

    static private boolean isContainedInto(ScheduleItem contained, ScheduleItem container) {
        return contained.startTime >= container.startTime &&
                contained.endTime <= container.endTime;
    }
}