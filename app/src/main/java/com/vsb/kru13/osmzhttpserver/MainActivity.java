package com.vsb.kru13.osmzhttpserver;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s;
    private static final int READ_EXTERNAL_STORAGE = 1;

    private Button startServer;
    private Button stopServer;
    private Button setThreads;
    private Button setCameraView;

    private CheckBox startService;

    private TextView ipAddressView;
    private TextView scrollableTextView;
    private TextView totalAmountView;
    private TextView totalThreadsView;

    private EditText editThreads;

    private ArrayList<String> messageList;
    private double totalAmountData;
    private int totalThreadsCount;

    private SocketServerService socketServerService;
    private Intent intent;
    private boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.startServer = (Button)findViewById(R.id.button1);
        this.startServer.setOnClickListener(this);
        this.stopServer = (Button)findViewById(R.id.button2);
        this.stopServer.setOnClickListener(this);
        this.setThreads = (Button)findViewById(R.id.buttonThreads);
        this.setThreads.setOnClickListener(this);
        this.setCameraView = (Button)findViewById(R.id.buttonCameraView);
        this.setCameraView.setOnClickListener(this);

        this.startService = (CheckBox)findViewById(R.id.checkbox_Service);

        this.ipAddressView = (TextView)findViewById(R.id.ipAddressView);
        this.scrollableTextView = (TextView)findViewById(R.id.scrollableTextView);
        this.totalAmountView = (TextView)findViewById(R.id.totalAmountView);
        this.totalThreadsView = (TextView)findViewById(R.id.totalThreadsView);

        this.editThreads = (EditText)findViewById(R.id.editThreads);

        this.messageList = new ArrayList<>();
        this.totalAmountData = this.totalThreadsCount = 0;

        this.socketServerService = null;
        this.intent = null;
        this.isBound = false;

        this.s = new SocketServer(this.messageHandler);
        this.clearUI(false);
        this.s = null;

//        this.startServer.callOnClick();
    }

    @SuppressLint("HandlerLeak")
    private Handler messageHandler = new Handler(Looper.getMainLooper()) {

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message inputMessage) {
            if(isBound || s != null && s.isRunning()) {
                super.handleMessage(inputMessage);

                if(inputMessage.getData().containsKey("IP"))
                    ipAddressView.setText(String.valueOf(inputMessage.getData().get("IP")));

                if(inputMessage.getData().containsKey("MESSAGE")) {
                    String loadedData = String.valueOf(inputMessage.getData().get("MESSAGE"));
                    messageList.add(loadedData);

                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = messageList.size(); i-- > 0; )
                        stringBuilder.append(messageList.get(i));
                    scrollableTextView.setText(stringBuilder.toString());
                }

                if(inputMessage.getData().containsKey("DATA")) {
                    totalAmountData += inputMessage.getData().getLong("DATA");
                    totalAmountView.setText("Total data transferred: " + (totalAmountData / 1000) + "kB");
                }

                if(inputMessage.getData().containsKey("THREAD")) {
                    totalThreadsCount += inputMessage.getData().getInt("THREAD");
                    if(isBound)
                        totalThreadsView.setText("Count of running threads: " + totalThreadsCount + "/" + socketServerService.getSocketServer().getMaxThreadsCount());
                    else
                        totalThreadsView.setText("Count of running threads: " + totalThreadsCount + "/" + s.getMaxThreadsCount());
                }
            }
        }
    };

    private void startSocketServer() {
        Log.d("MAIN", "Server not running, starting...");
        this.s = new SocketServer(this.messageHandler);
        this.s.start();
        this.clearUI(true);
    }

    private void startSocketServerService() {
        Log.d("MAIN", "Server Service not running, starting...");
        this.intent = new Intent(this.getBaseContext(), SocketServerService.class);
        startService(this.intent);
        this.doBindService();
    }

    private void closeSocketServer() {
        this.clearUI(false);
        this.s.close();

        try {
            this.s.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeSocketServerService() {
        this.clearUI(false);
        this.socketServerService.stopSocketServerService();
        this.doUnbindService();
    }

    private void clearUI(boolean active) {
        this.startServer.setEnabled(!active);
        this.stopServer.setEnabled(active);
        this.setThreads.setEnabled(active);
        this.startService.setEnabled(!active);

        clearTextViews(active);
    }

    @SuppressLint("SetTextI18n")
    private void clearTextViews(boolean active) {
        this.messageList = new ArrayList<>();
        this.totalAmountData = this.totalThreadsCount = 0;

        this.ipAddressView.setText("null");
        this.scrollableTextView.setText("There will be logs in here...");
        this.totalAmountView.setText("Total data transferred: " + this.totalAmountData + "kB");
        if(isBound)
            this.totalThreadsView.setText("Count of running threads: " + this.totalThreadsCount + "/" + (!active? "0" : this.socketServerService.getSocketServer().getMaxThreadsCount()));
        else
            this.totalThreadsView.setText("Count of running threads: " + this.totalThreadsCount + "/" + (!active? "0" : this.s.getMaxThreadsCount()));
        this.editThreads.setText("");
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button1) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {
                if(this.startService.isChecked())
                    this.startSocketServerService();
                else
                    this.startSocketServer();
            }
        }

        else if (v.getId() == R.id.button2) {
            if(isBound)
                this.closeSocketServerService();
            else
                this.closeSocketServer();
            }

        else if (v.getId() == R.id.buttonThreads) {
            String threads = this.editThreads.getText().toString();
            if (!threads.equals("")) {
                if(this.isBound && this.socketServerService.getSocketServer().extendThreadsPool(Integer.parseInt(threads)) || this.s != null && this.s.extendThreadsPool(Integer.parseInt(threads)))
                    this.totalThreadsView.setText("Count of running threads: " + this.totalThreadsCount + "/" + threads);
            }
            this.editThreads.setText("");
        }

        else if (v.getId() == R.id.buttonCameraView) {
            if(isBound)
                this.closeSocketServerService();
            else if(s != null && s.isRunning())
                this.closeSocketServer();

            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if(this.startService.isChecked())
                        this.startSocketServerService();
                    else
                        this.startSocketServer();
                }
                break;
            default:
                break;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            socketServerService = ((SocketServerService.LocalBinder)iBinder).getService();
            socketServerService.startSocketServerService(messageHandler);
            if(isBound)
                clearUI(true);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            socketServerService = null;
            doUnbindService();
        }
    };

    private void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        bindService(new Intent(this, SocketServerService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    private void doUnbindService() {
        if(isBound) {
            // Release information about the service's state.
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doUnbindService();
    }
}
