/*
    CanZE
    Take a closer look at your ZE car

    Copyright (C) 2015 - The CanZE Team
    http://canze.fisch.lu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.canze.activities;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

import lu.fisch.canze.R;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.actors.Fields;
import lu.fisch.canze.interfaces.FieldListener;

// If you want to monitor changes, you must add a FieldListener to the fields.
// For the simple activity, the easiest way is to implement it in the actitviy itself.
public class FluenceKangooTempsActivity extends CanzeActivity implements FieldListener {

    public static final String SID_EvaporatorTemperature                = "42a.30";
    public static final String SID_HVEvaporatorTemperature              = "430.40";
    public static final String SID_WaterTemperatureHeating              = "5da.0";
    public static final String SID_DcDcConverterTemperature             = "77e.623018.24";
    public static final String SID_InverterTemperature                  = "77e.62302b.24";
    public static final String SID_ExternalTemperature                  = "543.32";
    public static final String SID_ExternalTemperatureZoe               = "656.48";
    public static final String SID_InternalTemperature                  = "764.6121.8";
    public static final String SID_MotorWaterPumpSpeed                  = "7ec.623318.24";
    public static final String SID_ChargerWaterPumpSpeed                = "7ec.623319.24";
    public static final String SID_HeatingWaterPumpSpeed                = "7ec.62331a.24";
    // public static final String SID_BatteryCoolingFansSpeed              = "7bb.6101.335";

    private ArrayList<Field> subscribedFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fluence_kangoo_temps);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // initialise the widgets
        initListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeListeners();
    }

    private void initListeners() {

        subscribedFields = new ArrayList<>();

        if (MainActivity.car == MainActivity.CAR_ZOE) {
            addListener(SID_ExternalTemperatureZoe);
            addListener(SID_HVEvaporatorTemperature);
        } else {
            addListener(SID_ExternalTemperature);
        }
        addListener(SID_EvaporatorTemperature);
        addListener(SID_WaterTemperatureHeating);
        addListener(SID_DcDcConverterTemperature);
        addListener(SID_InverterTemperature);
        addListener(SID_InternalTemperature);
        addListener(SID_MotorWaterPumpSpeed);
        addListener(SID_ChargerWaterPumpSpeed);
        addListener(SID_HeatingWaterPumpSpeed);
    }

    private void removeListeners () {
        // empty the query loop
        MainActivity.device.clearFields();
        // free up the listeners again
        for (Field field : subscribedFields) {
            field.removeListener(this);
        }
        subscribedFields.clear();
    }

    private void addListener(String sid) {
        Field field;
        field = MainActivity.fields.getBySID(sid);
        if (field != null) {
            field.addListener(this);
            MainActivity.device.addActivityField(field);
            subscribedFields.add(field);
        } else {
            MainActivity.toast("sid " + sid + " does not exist in class Fields");
        }

    }



    // This is the event fired as soon as this the registered fields are
    // getting updated by the corresponding reader class.
    @Override
    public void onFieldUpdateEvent(final Field field) {
        // the update has to be done in a separate thread
        // otherwise the UI will not be repainted
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String fieldId = field.getSID();
                TextView tv = null;

                // get the text field
                switch (fieldId) {

                    case SID_EvaporatorTemperature:
                        tv = (TextView) findViewById(R.id.textEvaporatorTemperature);
                        break;
                    case SID_HVEvaporatorTemperature:
                        //tv = (TextView) findViewById(R.id.textHVEvaporatorTemperature);
                        break;
                    case SID_WaterTemperatureHeating:
                        tv = (TextView) findViewById(R.id.textWaterTemperatureHeating);
                        break;
                    case SID_DcDcConverterTemperature:
                        tv = (TextView) findViewById(R.id.textDcDcConverterTemperature);
                        break;
                    case SID_InverterTemperature:
                        tv = (TextView) findViewById(R.id.textInverterTemperature);
                        break;
                    case SID_ExternalTemperature:
                    case SID_ExternalTemperatureZoe:
                        tv = (TextView) findViewById(R.id.textExternalTemperature);
                        break;
                    case SID_InternalTemperature:
                        tv = (TextView) findViewById(R.id.textInternalTemperature);
                        break;
                    case SID_MotorWaterPumpSpeed:
                        tv = (TextView) findViewById(R.id.textMotorWaterPumpSpeed);
                        break;
                    case SID_ChargerWaterPumpSpeed:
                        tv = (TextView) findViewById(R.id.textChargerWaterPumpSpeed);
                        break;
                    case SID_HeatingWaterPumpSpeed:
                        tv = (TextView) findViewById(R.id.textHeatingWaterPumpSpeed);
                        break;
                    //case SID_BatteryCoolingFansSpeed:
                    //  tv = (TextView) findViewById(R.id.textBatteryCoolingFansSpeed);
                    //  break;
                }
                // set regular new content, all exeptions handled above
                if (tv != null) {
                    tv.setText("" + (Math.round(field.getValue() * 10.0) / 10.0));
                }

                tv = (TextView) findViewById(R.id.textDebug);
                tv.setText(fieldId);
            }
        });

    }
}