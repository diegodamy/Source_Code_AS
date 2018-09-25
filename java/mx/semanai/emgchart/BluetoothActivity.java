package mx.semanai.emgchart;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity{

    private static final int REQUEST_ENABLE_BT = 1 ;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter btadapter;
    private ListView listaBtPaired, listaBtNew;

    ArrayAdapter<String> btDevice, btDeviceNew;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btlist_fragment);

        btadapter = BluetoothAdapter.getDefaultAdapter();
        listaBtPaired = (ListView) findViewById(R.id.listaBt);
        listaBtNew = (ListView) findViewById(R.id.listaBtN);

        btDevice = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        btDeviceNew = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        if(btadapter == null ){
            Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
            finish();
        }

        //Set listView for paired devices.
        listaBtPaired.setAdapter(btDevice);
        listaBtPaired.setOnItemClickListener(mDeviceClickListener);
        //Set listView for new devices.
        listaBtNew.setAdapter(btDeviceNew);
        listaBtNew.setOnItemClickListener(mDeviceClickListener);

        // If we're already discovering, stop it
        if (btadapter.isDiscovering()) {
            btadapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        btadapter.startDiscovery();


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!btadapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device: pairedDevices){
                btDevice.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none).toString();
            btDevice.add(noDevices);
        }

        // Register for broadcasts when discovery has finished
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            btadapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);


            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra("address", address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If device is already bonded skip it
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    btDeviceNew.add(device.getName() + "\n" + device.getAddress());
                }
                // Add the name and address to an array adapter to show in a ListView
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                if (btDeviceNew.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none).toString();
                    btDeviceNew.add(noDevices);
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (btadapter != null) {
            btadapter.cancelDiscovery();
        }// Unregister broadcast listeners

        this.unregisterReceiver(mReceiver);

    }





}
