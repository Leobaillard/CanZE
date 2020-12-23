package lu.fisch.canze.classes;

import android.os.Handler;

import java.util.ArrayList;

import androidx.collection.ArrayMap;
import lu.fisch.canze.activities.MainActivity;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.interfaces.FieldListener;

public abstract class DataSharer implements FieldListener {
    public static int SEND_INTERVAL = 10000; // Time between two data send (ms)
    public static int POLL_INTERVAL = 10000; // Time between two data polls (ms)

    public static ArrayList<String> sharedFieldsSids = new ArrayList<>();
    public static ArrayList<Field> sharedFields;
    public static ArrayMap<String, Field> fieldValues = new ArrayMap<>();

    private static boolean allowPosition = false;
    private static boolean activated = false;
    private final Handler handler = new Handler();

    public DataSharer() {
        // Set shared fields SIDs
        setSharedSids();
    }

    @Override
    public void onFieldUpdateEvent(final Field field) {
        String fieldId = field.getSID();
        if (!fieldValues.containsKey(fieldId)) {
            fieldValues.put(fieldId, field);
        } else {
            fieldValues.setValueAt(fieldValues.indexOfKey(fieldId), field);
        }
    }

    ;

    public abstract boolean sendData();

    public abstract void setSharedSids();

    public void setAllowPosition(boolean status) {
        allowPosition = status;
    }

    public boolean getAllowPosition() {
        return allowPosition;
    }

    private void initListeners() {
        sharedFields = new ArrayList<>();
        for (String sid : sharedFieldsSids) {
            addListener(sid, POLL_INTERVAL);
        }
    }

    private void addListener(String sid, int pollInterval) {
        Field field;
        field = MainActivity.fields.getBySID(sid);

        MainActivity.debug("DataSharer: addListener");

        if (null != field) {
            field.addListener(this);
            MainActivity.device.addApplicationField(field, pollInterval);
            sharedFields.add(field);
        }
    }

    public boolean setActive(boolean status) {
        boolean result = status;

        if (activated != status) {
            if (status) {
                // Activate sharing, start timer
                result = start();
                activated = result;
            } else {
                // Stop sharing, stop timer
                result = stop();
                activated = false;
            }
        }

        return result;
    }

    private final Runnable onTimerTick = new Runnable() {
        @Override
        public void run() {
            MainActivity.debug("DataSharer: onTimerTick");
            sendData();

            handler.postDelayed(this, SEND_INTERVAL);
        }
    };

    public boolean start() {
        MainActivity.debug("DataSharer: start");
        try {
            // Start timer
            handler.postDelayed(onTimerTick, 400);

            // Init field listeners
            initListeners();
        } catch (Exception e) {
            MainActivity.toast(MainActivity.TOAST_NONE, "Could not start data sharing thread");

            return false;
        }

        return true;
    }

    public boolean stop() {
        MainActivity.debug("DataSharer: stop");
        try {
            // Stop timer
            handler.removeCallbacks(onTimerTick);

            // Free the field listeners
            if (null != sharedFields) {
                for (Field field : sharedFields) {
                    field.removeListener(this);
                }
                sharedFields.clear();
            }
        } catch (Exception e) {
            MainActivity.toast(MainActivity.TOAST_NONE, "Could not stop data sharing thread");
            MainActivity.logExceptionToCrashlytics(e);

            // We probably don't want to continue sharing data if we have been asked to stop
            throw e;
        }

        return true;
    }

    public void destroy()
    {
        MainActivity.debug("DataSharer: destroy");
        handler.removeCallbacks(onTimerTick);
        stop();
    }
}
