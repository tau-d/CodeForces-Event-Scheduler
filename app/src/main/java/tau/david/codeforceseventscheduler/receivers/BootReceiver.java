package tau.david.codeforceseventscheduler.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tau.david.codeforceseventscheduler.MainActivity;


public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MainActivity.TAG, "BootReceiver broadcast received!");

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent scheduleIntent = new Intent(context, SchedulerReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, scheduleIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000 * 60, AlarmManager.INTERVAL_HALF_DAY, alarmIntent);
    }
}
