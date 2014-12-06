package com.example.ali.raceandroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import li.vin.bt.Device;
import li.vin.bt.DeviceInterface;
import li.vin.bt.Pid;
import li.vin.bt.VinliDevices;
import li.vin.net.Vehicle;
import li.vin.net.VinliApp;
import rx.Observable;
import rx.Observer;
import rx.android.observables.AndroidObservable;
import rx.functions.Func1;
//import li.vin.net.VinliApp;

/**
 * Created by Ali on 11/7/14.
 */
public class PairingActivity extends Activity {


    private final VinliApp mVinli = VinliApp.create(
            "t51VHDUrapdv70ZQBT89",
            "3ca6df10-6d39-11e4-a28f-d7fa3f29e79f");

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.pairinglayout);
        final Context context = this;


        final TextView text = (TextView) findViewById(R.id.pairingtext);
        Button button = (Button) findViewById(R.id.pairingbutton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting device observable
             AndroidObservable.bindActivity(PairingActivity.this, VinliDevices.createDeviceObservable(getApplicationContext())
             .first()
             .map(new Func1<Device, DeviceInterface>() {
                 @Override
                 public DeviceInterface call(Device device) {

                     return device.createDeviceInterface(context);
                 }
             }).flatMap(new Func1<DeviceInterface, Observable<Integer>>() {
                 @Override
                 public Observable<Integer> call(DeviceInterface deviceInterface) {
                     return deviceInterface.observe(Pid.SPEED_MPH);
                 }
             })).subscribe(new Observer<Integer>() {
                 @Override
                 public void onCompleted() {
                 Log.d("PairingActivity", "On complete called");
                 }

                 @Override
                 public void onError(Throwable throwable) {

                 }
                 @Override
                 public void onNext(Integer integer) {
                        text.setText(String.valueOf(integer));
                 }
             });
                Log.d("SPEED_MPH", "DISPLAY RPM");
            }
        });

    }
}
