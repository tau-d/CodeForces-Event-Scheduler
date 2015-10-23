package tau.david.codeforceseventscheduler.settings;

import android.app.Activity;
import android.app.ListFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import tau.david.codeforceseventscheduler.R;

public class EventReminderSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_notification_settings);

        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_event_notification_fragment_container, new EventReminderListFragment())
                .commit();
    }

    private static final Pair[] reminderTypes = {
            new Pair("Default", CalendarContract.Reminders.METHOD_DEFAULT),
            new Pair("Alert", CalendarContract.Reminders.METHOD_ALERT)
    };

    // Time in minutes
    private static final Pair[] reminderTimes = {
            new Pair("0 minutes",   0),
            new Pair("5 minutes",   5),
            new Pair("10 minutes",  10),
            new Pair("15 minutes",  15),
            new Pair("20 minutes",  20),
            new Pair("25 minutes",  25),
            new Pair("30 minutes",  30),
            new Pair("45 minutes",  45),
            new Pair("1 hour",      60),
            new Pair("2 hours",     60 * 2),
            new Pair("3 hours",     60 * 3),
            new Pair("12 hours",    60 * 12),
            new Pair("24 hours",    60 * 24),
            new Pair("2 days",      60 * 24 * 2),
            new Pair("1 week",      60 * 24 * 7)
    };

    private static class EventReminderListAdapter extends BaseAdapter {
        List<EventReminder> items;
        SharedPreferences prefs;
        Activity mActivity;

        public EventReminderListAdapter(Activity activity) {
            this.mActivity = activity;
            this.items = new ArrayList<EventReminder>();
            prefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
            loadReminders();
        }

        private void loadReminders() {
            String reminders = prefs.getString(mActivity.getString(R.string.pref_key_reminders), null);

            if (reminders != null) {
                List<EventReminder> list = EventReminder.getEventReminderListFromString(reminders);
                items.addAll(list);
            }
        }

        public void newReminder() {
            items.add(new EventReminder());
            notifyDataSetChanged();
        }

        private void savePrefs() {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(mActivity.getApplicationContext().getString(R.string.pref_key_reminders),
                    EventReminder.getStringFromEventReminderList(items));
            editor.commit();
        }

        private int getSpinnerPos(Pair[] p, int val) {
            for (int i = 0; i < p.length; i++) {
                if (p[i].getValue() == val) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public View getView(final int itemPosition, View convertView, ViewGroup parent) {
            // TODO: View holder pattern

            LayoutInflater inflater = mActivity.getLayoutInflater();
            View view = inflater.inflate(R.layout.reminder_item, null);

            final EventReminder item = items.get(itemPosition);

            final Spinner typeSpinner = (Spinner) view.findViewById(R.id.reminder_type_spinner);
            final ArrayAdapter<Pair> typeAdapter = new ArrayAdapter<Pair>(mActivity, android.R.layout.simple_list_item_1, reminderTypes);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);
            typeSpinner.setSelection(getSpinnerPos(reminderTypes, item.getReminderType()));
            typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                    Pair type = typeAdapter.getItem(spinnerPosition);
                    item.setReminderType(type.getValue());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });

            final Spinner timeSpinner = (Spinner) view.findViewById(R.id.reminder_time_spinner);
            final ArrayAdapter<Pair> timesAdapter = new ArrayAdapter<Pair>(mActivity, android.R.layout.simple_list_item_1, reminderTimes);
            timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            timeSpinner.setAdapter(timesAdapter);
            timeSpinner.setSelection(getSpinnerPos(reminderTimes, item.getMinutesBefore()));
            timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                    Pair time = timesAdapter.getItem(spinnerPosition);
                    item.setMinutesBefore(time.getValue());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });


            Button delete = (Button) view.findViewById(R.id.delete_reminder_button);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    items.remove(itemPosition);
                    notifyDataSetChanged();
                }
            });

            return view;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public int getItemViewType(int position) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    private static class Pair {
        private String display;
        private int value;

        public Pair(String display, int value) {
            this.display = display;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public static class EventReminderListFragment extends ListFragment {
        private EventReminderListAdapter adapter;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            LayoutInflater inflater = getActivity().getLayoutInflater();
            adapter = new EventReminderListAdapter(getActivity());

            View footer = inflater.inflate(R.layout.activity_event_notification_footer, null);
            Button add = (Button) footer.findViewById(R.id.button_add_new_notification);
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.newReminder();
                }
            });

            ListView lv = getListView();
            lv.addFooterView(footer);
            lv.setDivider(null);
            lv.setDividerHeight(0);
            setListAdapter(adapter);
        }

        @Override
        public void onPause() {
            super.onPause();

            adapter.savePrefs();
        }
    }

}
