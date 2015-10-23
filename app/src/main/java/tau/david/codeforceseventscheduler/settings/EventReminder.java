package tau.david.codeforceseventscheduler.settings;


import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.List;

public class EventReminder {
    public static String TYPE_MINUTES_SEPARATOR = ",";
    public static String ITEM_SEPARATOR = ";";

    private int reminderType;
    private int minutesBefore;

    public EventReminder(int reminderType, int minutesBefore) {
        this.reminderType = reminderType;
        this.minutesBefore = minutesBefore;
    }

    public EventReminder() {
        this(CalendarContract.Reminders.METHOD_DEFAULT, 30);
    }

    public EventReminder(String reminderItemString) {
        String[] split = reminderItemString.split(TYPE_MINUTES_SEPARATOR);
        if (split.length != 2) throw new IllegalArgumentException();
        this.reminderType = Integer.parseInt(split[0]);
        this.minutesBefore = Integer.parseInt(split[1]);
    }

    public int getReminderType() {
        return reminderType;
    }

    public void setReminderType(int reminderType) {
        this.reminderType = reminderType;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public void setMinutesBefore(int minutesBefore) {
        this.minutesBefore = minutesBefore;
    }

    @Override
    public int hashCode() {
        return reminderType * 51 + minutesBefore * 37;
    }

    @Override
    public String toString() {
        return reminderType + TYPE_MINUTES_SEPARATOR + minutesBefore;
    }

    public static List<EventReminder> getEventReminderListFromString(String itemListString) {
        ArrayList<EventReminder> ans = new ArrayList<EventReminder>();

        String[] items = itemListString.split(ITEM_SEPARATOR);
        for (String item : items) {
            ans.add(new EventReminder(item));
        }

        return ans;
    }

    public static String getStringFromEventReminderList(List<EventReminder> list) {
        if (list.isEmpty()) return null;

        StringBuilder ans = new StringBuilder();
        for (EventReminder er : list) {
            ans.append(er.toString());
            ans.append(ITEM_SEPARATOR);
        }
        ans.deleteCharAt(ans.length() - 1);

        return ans.toString();
    }
}