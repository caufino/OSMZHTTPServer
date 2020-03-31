package com.vsb.kru13.osmzhttpserver;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    private final int port = 12345;
    private boolean bRunning;
    private Handler messageHandler;
    private DynamicSemaphore semaphore;

    public SocketServer(Handler messageHandler) {
        Log.d("SERVER", "SocketServer Init");
        this.messageHandler = messageHandler;
        this.semaphore = new DynamicSemaphore(1, 2);
    }

    public synchronized boolean extendThreadsPool(int newThreadsCount) {
        return this.semaphore.extendThreadsPool(newThreadsCount);
    }

    public synchronized int getMaxThreadsCount() {
        return this.semaphore.getThreadsCount();
    }

    public boolean isRunning() {
        return bRunning;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Waiting for connection accept...");
                Socket s = serverSocket.accept();

                if(this.messageHandler == null) {
                    Log.d("SERVER", "CAMERA CLIENT HANDLER");
                    CameraThread cameraThread = new CameraThread(s);
                    cameraThread.start();
                }
                else if(this.semaphore.tryAcquire()) {
                    Log.d("SERVER", "Connection accepted, starting thread...");
                    ClientThread clientThread = new ClientThread(s, this.messageHandler, this.semaphore);
                    clientThread.start();
                }
                else {
                    Log.d("ERROR", "Server too busy");
                    Log.d("SERVER", "Closing connection...");
                    s.close();
                }
            }
        }
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }
}
