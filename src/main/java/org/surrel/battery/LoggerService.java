package org.surrel.battery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;

public class LoggerService extends IntentService {
    public static final String ACTION_START_LOGGING = ".START_LOGGING";
    private static final String ACTION_STOP_LOGGING = ".STOP_LOGGING";
    public static final String ACTION_MEASURE = ".MEASURE";

    public static final String EXTRA_RESULT_RECEIVER = "resultReceiver";

    static final int TIMEOUT_MSEC = 10 * 1000;
    static final int LOG_INTERVAL = 5 * 60 * 1000;

    public LoggerService() {
        super("LoggerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle ret = null;

        if (ACTION_START_LOGGING.equals(intent.getAction())) {
            startLogging();
        } else if (ACTION_STOP_LOGGING.equals(intent.getAction())) {
            stopLogging();
        } else if (ACTION_MEASURE.equals(intent.getAction())) {
            measure();
        }

        Parcelable v = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (v != null && v instanceof ResultReceiver) {
            ((ResultReceiver) v).send(0, ret);
        }
    }

    private void startLogging() {
        LogData log = LogData.getInstance(this);
        long lasttime = log.getLastTimestamp();
        long nexttime = (lasttime > 0 ? lasttime + LOG_INTERVAL :
                System.currentTimeMillis() + LOG_INTERVAL);

        AlarmManager amgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        amgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                nexttime, LOG_INTERVAL, getMeasureIntent());
    }

    private void stopLogging() {
        AlarmManager amgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        amgr.cancel(getMeasureIntent());
    }

    private PendingIntent getMeasureIntent() {
        Intent intent = new Intent(this, Receiver.class)
                .setAction(ACTION_MEASURE);
        return PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void measure() {
        Intent lastIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (lastIntent != null) {
            int temp = lastIntent.getExtras().getInt(BatteryManager.EXTRA_TEMPERATURE);
            LogData log = LogData.getInstance(this);
            log.writeRecord(new LogData.LogRecord(System.currentTimeMillis(), temp));
        }
    }

}
