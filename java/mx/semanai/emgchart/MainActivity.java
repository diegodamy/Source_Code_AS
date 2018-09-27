package mx.semanai.emgchart;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //DEBUG
    private static final String TAG = "BluetoothSignal";

    private float xValue = 1;

    //GLOBAL VARIABLES
    private List<Entry> yValues; //Entries of signal.
    private Point[] dataPlot = {
            new Point(0,0)
    };
    private LineDataSet dataSet;  //set of data.
    private List<ILineDataSet> dataSets; // set of data.
    private LineData data; //data to plot.
    private XAxis xAxis;
    private StringBuilder sb;

    private StorageReference storage;

    //Layout Components
    private LineChart chart;
    private Button btButton, btnLoad;
    private TextView textBt;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    //Name of the connected device
    private  String mConnectedDeviceName = null;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter btAdapter = null;
    private BluetoothService btService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGraph();



        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null ){
            Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_LONG).show();
            finish();
        }

        storage = FirebaseStorage.getInstance().getReference();
        btButton = (Button) findViewById(R.id.btConnect);
        btnLoad = (Button) findViewById(R.id.btnLoad);
        textBt = (TextView) findViewById(R.id.btSelected);


        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        if (!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }

//        createFileAndUpload();
//        upload();

    }


    @Override
    protected void onStart() {
        super.onStart();

        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }else if(btService == null){
            //Toast.makeText(this,"Hola",Toast.LENGTH_SHORT).show();
            setupGraph();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                setupGraph();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(btService != null){
            btService.stop();
        }
    }

    public void initGraph(){

        chart = (LineChart) findViewById(R.id.chart);

        //Chart format

        chart.getDescription().setEnabled(false);

        //Place xAxis below chart and adjust startValue.
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
        //xAxis.setAxisMaximum(10f);

        //Remove right Y axis.
        YAxis rightYAxis = chart.getAxisRight();
        rightYAxis.setEnabled(false);

        //Adjust yAxis values.
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMaximum(5f);
        yAxis.setAxisMinimum(0f);
        yAxis.setDrawGridLines(false);

        //Remove legend.
        Legend legend = chart.getLegend();
        legend.setEnabled(false);


        //Entries List.
        yValues = new ArrayList<>();

//        for(Point data: dataPlot){
//            yValues.add(new Entry(data.x,data.y));
//        }

        // DataSet for entries.
        dataSet = new LineDataSet(yValues, "mV");

        // DataSet Line format.
        dataSet.setColor(Color.RED);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        //In case of more than 1 dataset.
        dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        data = new LineData(dataSets);

        //Set data to the chart.
//        chart.setData(data);
//        chart.invalidate();

//        List<ILineDataSet> dataSets = new ArrayList<>();
//        dataSets.add(dataSet);
//
//        LineData data = new LineData(dataSets);
//
//        chart.setData(data);
    }

    public void updateGraph(float x, float y){
        dataSet.addEntry(new Entry(x,y));
        chart.notifyDataSetChanged(); //notify the chart that there's new value
        //Toast.makeText(getApplicationContext(), Integer.toString(dataSet.getEntryCount()),Toast.LENGTH_SHORT).show();
        chart.fitScreen();
        xAxis.setAxisMaximum(dataSet.getEntryCount());
        chart.invalidate();
    }

    public int convertDataFromBT(String s){

        return 2;
    }

    /**
     * Creates a CSV file and saves it to the externalstorage
     *
     *  values List with data to save.
     */

    private void createFileAndUpload(){

        String COMMA_DELIMITER = ",";
        String LINE_SEPARATOR = "\n";
        String FILE_HEADER = "sample,value";

        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"PruebasSignal");
        if(!root.exists()){
            boolean wasSuccessful = root.mkdirs();
            if(!wasSuccessful){
                Toast.makeText(this,"No root created",Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,"Root Exist",Toast.LENGTH_SHORT).show();
        }

        File csv = new File(root,"prueba.csv");


        List<Valores> valuesToFile = new ArrayList<>();
        valuesToFile.add(new Valores(0,2));
        valuesToFile.add(new Valores(1,3));
        valuesToFile.add(new Valores(2,4));
        valuesToFile.add(new Valores(3,5));
        valuesToFile.add(new Valores(4,6));

        try {
            FileWriter file = new FileWriter(csv);
            file.append(FILE_HEADER);
            for (Valores val: valuesToFile){
                file.append(LINE_SEPARATOR);
                file.append(Integer.toString(val.getSample()));
                file.append(COMMA_DELIMITER);
                file.append(Float.toString(val.getVal()));
                file.append(COMMA_DELIMITER);
            }
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"File not created",Toast.LENGTH_SHORT).show();
        }

//        StorageReference fileRef = storage.child("prueba/prueba.csv");
//        Uri fileToUpload = Uri.fromFile(csv);
//
//        fileRef.putFile(fileToUpload)
//                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getApplicationContext(),"Failed",Toast.LENGTH_SHORT).show();
//                    }
//                });
//        UploadTask uploadTask = storageRef.putFile(fileToUpload);
//
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(getApplicationContext(), "Not able to upload", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Toast.makeText(getApplicationContext(),"Se subio esta madre",Toast.LENGTH_SHORT).show();
//            }
//        });

//        boolean delete = csv.delete();
//        if(!delete){
//            Toast.makeText(this,"File not deleted",Toast.LENGTH_SHORT).show();
//        }

    }

    private void upload(){

        Uri fileToUpload = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PruebasSignal/prueba.csv"));
        StorageReference fileRef = storage.child("prueba/prueba.csv");
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");

        fileRef.putFile(fileToUpload)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),"Failed",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = 100 * (taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                        progressDialog.setMessage(((int)progress)+"% Uploaded...");
                    }
                });
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupGraph() {
        Log.d(TAG, "setupGraph()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BluetoothService(getApplicationContext(), mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (btService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.none, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            btService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //TODO OutTextEdit.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };


    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {

        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    Handler mHandler = new Handler(new IncomingHandlerCallback());

    class IncomingHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus("Connecting to" + mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    textBt.setText(readMessage);
                    //convertDataFromBT(readMessage);

                    if(Character.isDigit(readMessage.charAt(0)) || readMessage.equals("V")) {
                        sb.append(readMessage);//append string
                        if(readMessage.equals("")){

                        }

                        updateGraph(dataSet.getEntryCount(), Integer.parseInt(readMessage));
                        xValue += 0.1;
                    }
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    break;
            }

            return true;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                //Toast.makeText(getApplicationContext(),resultCode,Toast.LENGTH_LONG).show();
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //Toast.makeText(getApplicationContext(), resultCode, Toast.LENGTH_SHORT).show();
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupGraph();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.none,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }
    }


    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link BluetoothActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString("address");
        Toast.makeText(getApplicationContext(),address,Toast.LENGTH_SHORT).show();
        // Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btService.connect(device, secure);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

}
