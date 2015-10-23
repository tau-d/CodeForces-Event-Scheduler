package tau.david.codeforceseventscheduler.receivers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import tau.david.codeforceseventscheduler.MainActivity;
import tau.david.codeforceseventscheduler.R;
import tau.david.codeforceseventscheduler.settings.EventReminder;


public class SchedulerTask extends AsyncTask<Void, Void, Void> {
    private final Context context;
    private final List<String> toasts;

    public SchedulerTask(Context context) {
        this.context = context;
        this.toasts = new ArrayList<String>();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        for (String msg : toasts) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Document doc = Jsoup.connect("http://codeforces.com/contests").timeout(1000 * 30).get();

            Elements tables = doc.getElementsByTag("table");
            Element upcoming_contests = tables.get(0);
            Elements rows = upcoming_contests.getElementsByTag("tr");
            for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
                Element row = rows.get(rowNum);
                Elements columns = row.getElementsByTag("td");

                String eventName = columns.get(0).text().trim();
                String startString = columns.get(2).text().trim();
                String durationString = columns.get(3).text().trim();

                // Remove "Enter »" that appears when event starts
                String enter = "Enter »";
                if (eventName.endsWith(enter)) {
                    eventName = eventName.substring(0, eventName.length() - enter.length()).trim();
                }

                long startMillis = getStartMillis(startString);
                long endMillis = getEndMillis(startMillis, durationString);

                Cursor existingEvents = getExistingEventsCursor(eventName);
                existingEvents.moveToFirst();
                if (existingEvents.getCount() > 0) {
                    // Should only be one event with same name
                    if (wasRescheduled(existingEvents, startMillis, endMillis)) {
                        // Update existing event entry, do not insert again
                        updateEvent(existingEvents.getLong(existingEvents.getColumnIndex(CalendarContract.Events._ID)),
                                startMillis, endMillis, eventName);
                    }
                    existingEvents.close();
                    continue;
                }
                existingEvents.close();

                if (checkEventPreferences(eventName)) {
                    Uri event = insertEvent(eventName, startMillis, endMillis);
                    long eventId = Long.parseLong(event.getLastPathSegment());
                    insertAllReminders(eventId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private long getStartMillis(String startString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd/yyyy kk:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(sdf.parse(startString).getTime());
        start.add(Calendar.HOUR_OF_DAY, 1); // fix off by 1 hour Europe/Moscow timezone

        return start.getTimeInMillis();
    }

    private long getEndMillis(long startMillis, String durationString) {
        String[] split = durationString.split(":");
        int hours = Integer.parseInt(split[0]);
        int minutes = Integer.parseInt(split[1]);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(startMillis);
        end.add(Calendar.HOUR_OF_DAY, hours);
        end.add(Calendar.MINUTE, minutes);

        return end.getTimeInMillis();
    }

    private Cursor getExistingEventsCursor(String eventName) {
        ContentResolver cr = context.getContentResolver();
        String[] projection = {};
        String selection = CalendarContract.Events.TITLE + "=? AND " +
                CalendarContract.Events.CALENDAR_ID + "=?";
        String[] args = { eventName, getCalendarId() + ""};

        return cr.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null);
    }

    private boolean wasRescheduled(Cursor existingEvents, long startMillis, long endMillis) {
        boolean ans = existingEvents.getLong(existingEvents.getColumnIndex(CalendarContract.Events.DTSTART)) != startMillis
                || existingEvents.getLong(existingEvents.getColumnIndex(CalendarContract.Events.DTEND)) != endMillis;

        if (ans) {
            Log.i(MainActivity.TAG, existingEvents.getString(existingEvents.getColumnIndex(CalendarContract.Events.TITLE)) + " event was rescheduled!");
        } else {
            Log.i(MainActivity.TAG, existingEvents.getString(existingEvents.getColumnIndex(CalendarContract.Events.TITLE)) + " event is already scheduled correctly!");
        }

        return ans;
    }

    private Uri insertEvent(String name, long startMillis, long endMillis) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, name);
        values.put(CalendarContract.Events.CALENDAR_ID, getCalendarId());
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());

        Uri event = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        Log.i(MainActivity.TAG, name + " event inserted!");

        toasts.add(context.getString(R.string.toast_new_event) + "\n" + name);

        return event;
    }

    private void updateEvent(long eventId, long startMillis, long endMillis, String eventName) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        String where = CalendarContract.Events._ID + "=?";
        String[] args = { eventId + "" };

        cr.update(CalendarContract.Events.CONTENT_URI, values, where, args);

        Log.i(MainActivity.TAG, eventName + " event time updated!");
    }

    private void insertReminder(long eventId, int reminderType, int minutes) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.MINUTES, minutes);
        values.put(CalendarContract.Reminders.EVENT_ID, eventId);
        values.put(CalendarContract.Reminders.METHOD, reminderType);

        cr.insert(CalendarContract.Reminders.CONTENT_URI, values);
        Log.i(MainActivity.TAG, "Event " + eventId + ": " + minutes + " reminder inserted!");
    }

    private void insertAllReminders(long eventId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String reminders = prefs.getString(context.getString(R.string.pref_key_reminders), null);

        if (reminders != null) {
            List<EventReminder> list = EventReminder.getEventReminderListFromString(reminders);
            for (EventReminder er : list) {
                insertReminder(eventId, er.getReminderType(), er.getMinutesBefore());
            }
        }
    }

    private int getCalendarId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(context.getString(R.string.pref_key_calendar), "1"));
    }

    private boolean checkEventPreferences(String eventName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (eventName.contains("(Div. 1)")) {
            if (prefs.getBoolean(context.getString(R.string.pref_key_div1), true)) {
                return true;
            }
        } else if (eventName.contains("(Div. 2)")) {
            if (prefs.getBoolean(context.getString(R.string.pref_key_div2), true)) {
                return true;
            }
        } else {
            // Other
            if (prefs.getBoolean(context.getString(R.string.pref_key_other), true)) {
                return true;
            }
        }

        Log.i(MainActivity.TAG, "Event type excluded: " + eventName);
        return false;
    }
}
