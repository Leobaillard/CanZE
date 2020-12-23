package lu.fisch.canze.classes;

import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lu.fisch.canze.activities.MainActivity;
import lu.fisch.canze.activities.SettingsActivity;
import lu.fisch.canze.actors.Field;

public class AbrpDataSharer extends DataSharer {
    public static final String ENDPOINT_URL = "https://api.iternio.com/1/tlm/send";
    public static final String ENDPOINT_KEY = "0a064017-22e3-4be5-841f-d73d74d8fbe5"; // FIXME (LB's key)
    public static final int SEND_INTERVAL = 5000;
    public String carModel = "";
    public String userToken;

    /* ****************************
     * Singleton stuff
     * ****************************/
    private final static AbrpDataSharer instance = new AbrpDataSharer();

    private AbrpDataSharer() {
        // Set shared fields
        setSharedSids();

        // Set car model
        setCarModel();
    }

    public static AbrpDataSharer getInstance() {
        return instance;
    }

    /* ****************************
     * AbrpDataSharer stuff
     * ****************************/

    @Override
    public void setSharedSids() {
        sharedFieldsSids = new ArrayList<>();

        sharedFieldsSids.add(Sid.SoC);
        sharedFieldsSids.add(Sid.RealSpeed);

        if (getAllowPosition()) {
            sharedFieldsSids.add(Sid.GPSPosition); // lat/lon/alt - to be parsed
        }

        sharedFieldsSids.add(Sid.DcPowerOut);
        sharedFieldsSids.add(Sid.ChargingPower);
        sharedFieldsSids.add(Sid.PlugConnected);
        sharedFieldsSids.add(Sid.CCSEVSEPresentCurrent);
        sharedFieldsSids.add(Sid.AvailableEnergy);
        sharedFieldsSids.add(Sid.SOH);
        sharedFieldsSids.add(Sid.SoC);
        sharedFieldsSids.add(Sid.TractionBatteryVoltage);
        sharedFieldsSids.add(Sid.TractionBatteryCurrent);
        sharedFieldsSids.add(Sid.AverageBatteryTemperature);
        sharedFieldsSids.add(Sid.OutsideTemperature);
    }

    public void setCarModel() {
        // FIXME: ABRP distinguishes between motor variants
        switch (MainActivity.car) {
            case MainActivity.CAR_X10PH2:
                carModel = "renault:zoe:20:52:r110";
                break;
            case MainActivity.CAR_ZOE_Q210:
                carModel = "renault:zoe:q210:22:other";
                break;
            case MainActivity.CAR_ZOE_Q90:
                carModel = "renault:zoe:q90:40:other";
                break;
            case MainActivity.CAR_ZOE_R240:
                carModel = "renault:zoe:r240:22:other";
                break;
        }
    }

    public boolean sendData() {
        MainActivity.debug("DataSharer: sendData");
        // Check user token
        final String userToken = MainActivity.getInstance().settings.getString(SettingsActivity.SETTING_SHARING_ABRP_TOKEN, "");
        if (userToken.equals("")) {
            return false;
        }

        // Create the JSON object with field values
        final JSONObject data = new JSONObject();

        try {
            for (Map.Entry<String, Field> fieldValue : fieldValues.entrySet()) {
                switch (fieldValue.getKey()) {
                    case Sid.SoC:
                        data.put("soc", fieldValue.getValue().getValue());
                        break;
                    case Sid.RealSpeed:
                        data.put("speed", fieldValue.getValue().getValue());
                        break;
                    case Sid.GPSPosition:
                        // Split values
                        String[] position = fieldValue.getValue().getStringValue().split("/");
                        if (position.length == 2) {
                            data.put("lat", Double.parseDouble(position[0]));
                            data.put("lon", Double.parseDouble(position[1]));
                        }
                        if (position.length == 3) {
                            data.put("elevation", Double.parseDouble(position[2]));
                        }
                        break;
                    case Sid.AvailableEnergy:
                        data.put("battery_capacity", fieldValue.getValue().getValue());
                        break;
                    case Sid.OutsideTemperature:
                        data.put("ext_temp", fieldValue.getValue().getValue());
                        break;
                    case Sid.AverageBatteryTemperature:
                        data.put("batt_temp", fieldValue.getValue().getValue());
                        break;
                    case Sid.TractionBatteryVoltage:
                        data.put("voltage", fieldValue.getValue().getValue());
                        break;
                    case Sid.TractionBatteryCurrent:
                        data.put("current", fieldValue.getValue().getValue());
                        break;
                }
            }

            // Handle charging
            if (fieldValues.containsKey(Sid.PlugConnected)) {
                if (fieldValues.containsKey(Sid.ChargingPower)) {
                    double chargingPower = fieldValues.get(Sid.ChargingPower).getValue();
                    data.put("is_charging", (fieldValues.get(Sid.PlugConnected).getValue() == 1 && chargingPower > 0));
                }

                if (fieldValues.containsKey(Sid.CCSEVSEPresentCurrent)) {
                    double presentCurrent = fieldValues.get(Sid.CCSEVSEPresentCurrent).getValue();
                    data.put("is_dcfc", (fieldValues.get(Sid.PlugConnected).getValue() == 1 && presentCurrent > 0));
                }
            }

            // Set car model
            if (carModel.equals("")) {
                return false;
            } else {
                data.put("car_model", carModel);
            }

            // Set timestamp
            int timestamp = (int) System.currentTimeMillis();
            data.put("utc", timestamp);
        } catch (JSONException e) {
            return false;
        }

        // Send data to API
        // FIXME: get rid of the Volley dependency?
        RequestQueue queue = Volley.newRequestQueue(MainActivity.getInstance());
        StringRequest request = new StringRequest(
                Request.Method.POST,
                ENDPOINT_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // TODO: parse response and handle errors
                        MainActivity.debug("ABRP sharer HTTP response: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: better handle error
                        MainActivity.debug("HTTP ERROR: " + error.getMessage());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                try {
                    params.put("tlm", URLEncoder.encode(data.toString(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    MainActivity.debug("ABRP sharer ERROR while url encode: " + e.getMessage());
                }
                params.put("token", userToken);

                return params;
            }
        };

        // FIXME: this is async so the result needs to be handled in an other way
//        queue.add(request);
        MainActivity.debug("DEBUG HTTP POST: " + data.toString());

        return true;
    }
}
