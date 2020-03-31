package com.vsb.kru13.osmzhttpserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SocketServerService extends Service {
    private SocketServer socketServer;
    private final IBinder iBinder = new LocalBinder();
    private Handler handler = null;

    @Override
    public void onCreate() {
        // The service is being created
        Log.d("SocketServerService".toUpperCase(), "Creating SocketServerService");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.d("SocketServerService".toUpperCase(), "Starting SocketServerService");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return this.iBinder;
    }

//    @Override
//    public boolean onUnbind(Intent intent) {
//        // All clients have unbound with unbindService()
//        return mAllowRebind;
//    }

//    @Override
//    public void onRebind(Intent intent) {
//        // A client is binding to the service with bindService(),
//        // after onUnbind() has already been called
//    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        super.onDestroy();
        stopSocketServerService();
    }

    public void startSocketServerService(Handler handler) {
        this.handler = handler;

        this.socketServer = new SocketServer(this.handler);
        this.socketServer.start();
    }

    public void stopSocketServerService() {
        this.handler = null;
        closeSocketServerService();
        stopForeground(true);
        stopSelf();
    }

    private void closeSocketServerService() {
        if(this.socketServer != null && this.socketServer.isRunning()) {
            Log.d("SocketServerService".toUpperCase(), "Stopping SocketServerService");
            this.socketServer.close();
            try {
                this.socketServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class LocalBinder extends Binder {
        public SocketServerService getService() {
            return SocketServerService.this;
        }
    }

    public SocketServer getSocketServer() {
        return socketServer;
    }
}
