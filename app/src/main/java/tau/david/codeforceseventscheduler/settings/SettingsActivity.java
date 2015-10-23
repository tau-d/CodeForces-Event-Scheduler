package tau.david.codeforceseventscheduler.settings;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;

import tau.david.codeforceseventscheduler.R;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_settings_fragment_container, new SettingsActivityFragment())
                .commit();
    }

    public static class SettingsActivityFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();

            PreferenceScreen screen = getPreferenceScreen();
            ListPreference calendars = (ListPreference) screen.findPreference(getString(R.string.pref_key_calendar));
            Cursor c = getCalendarsCursor();
            calendars.setEntries(cursorToEntries(c));
            calendars.setEntryValues(cursorToEntryValues(c));
            c.close();
        }

        private Cursor getCalendarsCursor() {
            ContentResolver cr = getActivity().getContentResolver();
            String[] projection = { CalendarContract.Calendars.NAME, CalendarContract.Calendars._ID };
            return cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
        }

        private String[] cursorToEntries(Cursor cursor) {
            String[] ret = new String[cursor.getCount()];
            int index = 0;
            int col = cursor.getColumnIndex(CalendarContract.Calendars.NAME);

            cursor.moveToFirst();
            do {
                ret[index++] = cursor.getString(col);
            } while(cursor.moveToNext());
            return ret;
        }

        private String[] cursorToEntryValues(Cursor cursor) {
            String[] ret = new String[cursor.getCount()];
            int index = 0;
            int col = cursor.getColumnIndex(CalendarContract.Calendars._ID);

            cursor.moveToFirst();
            do {
                ret[index++] = cursor.getString(col);
            } while(cursor.moveToNext());
            return ret;
        }
    }
}

