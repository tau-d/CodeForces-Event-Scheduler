package tau.david.codeforceseventscheduler.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tau.david.codeforceseventscheduler.MainActivity;


public class SchedulerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(MainActivity.TAG, "Scheduler broadcast received!");

        SchedulerTask task = new SchedulerTask(context);
        task.execute();
    }
}
