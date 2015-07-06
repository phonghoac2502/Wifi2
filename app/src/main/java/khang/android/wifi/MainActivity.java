package khang.android.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import khang.android.wifi.data.ACAdapter;
import khang.android.wifi.data.AccessPoint;
import khang.android.wifi.data.AccessPointRepo;

public class MainActivity extends Activity {
    private WifiManager mainWifi;
    private WifiReceiver receiverWifi;
    private ACAdapter acAdapter;
    private ArrayList<AccessPoint> wifiData = new ArrayList<AccessPoint>();
    private final android.os.Handler handler = new android.os.Handler();
    private static final int RESET_TIME = 500;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (!mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(true);
        }
        doInback();
    }
    @Override
    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(receiverWifi);
        super.onStop();
    }

    @Override
    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        if (mainWifi != null && mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(false);
        }
        super.onDestroy();
    }

    public void doInback() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                receiverWifi = new WifiReceiver();
                registerReceiver(receiverWifi,
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                mainWifi.startScan();
                doInback();
            }
        }, RESET_TIME);
    }

    public void InsertDB(int userInput) {
        final AccessPointRepo repo = new AccessPointRepo(this);
        ArrayList<AccessPoint> accessPointArrayList = wifiData;
        AccessPoint accessPoint = null;
        AccessPoint accessPoint1;
        for (int i = 0; i < accessPointArrayList.size(); i++) {
            accessPoint1 = wifiData.get(i);
            accessPoint.macaddress = accessPoint1.getMAC();
            accessPoint.rssi = accessPoint1.getLeveldBm();
            accessPoint.locationID = userInput;
            repo.insert(accessPoint);
        }
    }
    public void onCollectBtn_Click(View view) {
        LayoutInflater li = LayoutInflater.from(this);
        View dialogInput = li.inflate(R.layout.dialog_input, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(dialogInput);
        final EditText userInput = (EditText) dialogInput
                .findViewById(R.id.editTextDialogUserInput);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ArrayList<AccessPoint> accessPointArrayList = wifiData;
                                final int uI = Integer.parseInt(userInput.getText().toString());
                                CountDownTimer waitTimer = new CountDownTimer(5000, 1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        InsertDB(uI);
                                    }
                                    @Override
                                    public void onFinish() {
                                    }
                                }.start();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            ArrayList<ScanResult> wifiList = new ArrayList<ScanResult>(mainWifi.getScanResults());
            wifiData.clear();
            for (int i = 0; i < wifiList.size(); i++) {
                wifiData.add(new AccessPoint(wifiList.get(i)));
            }
            if (acAdapter == null) {
                acAdapter = new ACAdapter(context, R.layout.ac_list_item, wifiData);
                ListView listView = (ListView) findViewById(R.id.lstview_AC);
                listView.setAdapter(acAdapter);
            } else {
                acAdapter.notifyDataSetChanged();
            }
        }
    }
}
