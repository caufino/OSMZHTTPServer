package com.vsb.kru13.osmzhttpserver;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Semaphore;

class ClientThread extends Thread {
    private Socket socket;
    private String inputLine, page_file;
    private Handler messageHandler;
    private Message message;
    private StringBuilder messageStringBuilder;
    private Bundle messageBundle;
    private Semaphore semaphore;

    public ClientThread(Socket socket, Handler messageHandler, Semaphore semaphore) {
        this.socket = socket;
        this.messageHandler = messageHandler;
        this.inputLine = this.page_file = "";
        this.semaphore = semaphore;
    }

    public void run() {
        try {
            this.messageStringBuilder = new StringBuilder();
            this.messageBundle = new Bundle();
            this.message = new Message();

            this.messageBundle.putInt("THREAD", 1);
            this.messageStringBuilder.append(this.socket.getInetAddress().getHostAddress() + ":"
                    + this.socket.getLocalPort());
            this.messageBundle.putString("IP", this.messageStringBuilder.toString());
            this.message.setData(this.messageBundle);
            this.messageHandler.sendMessage(message);

            this.messageStringBuilder = new StringBuilder();
            this.messageBundle = new Bundle();
            this.message = new Message();

            Log.d("CLIENTTHREAD", "Socket Accepted, initializing...");
            this.messageStringBuilder.append("THREAD: Socket Accepted, initializing...\n");

            OutputStream o = this.socket.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            Log.d("CLIENTTHREAD", "Parsing header...");
            this.messageStringBuilder.append("THREAD: parsing header...\n");

            while((this.inputLine = in.readLine()) != null && !this.inputLine.isEmpty()) {
                if(this.inputLine.contains("HTTP") && this.page_file.equals("")) {
                    this.page_file = parseHeader(this.inputLine);
                    break;
                }
            }

            Log.d("CLIENTTHREAD", "Header parsed successfully, looking for a page...");

            File sdcard = Environment.getExternalStorageDirectory();
            File page = loadPage(sdcard, this.page_file);

            if(page != null) {
                FileInputStream file = new FileInputStream(page);
                String contentType;

                if(this.page_file.contains(".html"))
                    contentType = "Content-Type: text/html\n";
                else if(this.page_file.contains(".png"))
                    contentType = "Content-Type: image/png\n";
                else if(this.page_file.contains(".jpg"))
                    contentType = "Content-Type: image/jpeg\n";
                else
                    contentType = "Content-Type: text/html\n";

                Log.d("CLIENTTHREAD", "Loading requested page: " + this.page_file);
                this.messageStringBuilder.append("THREAD: 200, loading requested page `" + this.page_file + "`\n");

                out.write("HTTP/1.1 200 OK\n" +
                        contentType +
                        "Content-Length:" + page.length() + "\n" +
                        "\n");

                o.flush();
                out.flush();

                byte[] buffer = new byte[1024];
                int length;
                while ((length = file.read(buffer)) > 0) {
                    o.write(buffer, 0, length);
                }

                this.messageBundle.putLong("DATA", page.length());
            }
            else {
                Log.d("CAMERATHREAD", "404: Page not found!");
                this.messageStringBuilder.append("THREAD: 404, requested page `").append(this.page_file).append("` not found!\n");
                out.write(failPage());
            }

            o.flush();
            out.flush();

            //messageHandler.obtainMessage();

            this.socket.close();
            o.close();
            out.close();
            in.close();

            Log.d("CLIENTTHREAD", "Socket Closed");
            this.messageStringBuilder.append("THREAD: closing socket...\n");
            this.messageStringBuilder.append("-------------------------------------------------------------------------------------------------\n");
            this.messageBundle.putString("MESSAGE", this.messageStringBuilder.toString());
            this.messageBundle.putInt("THREAD", -1);

            this.message.setData(this.messageBundle);
            this.messageHandler.sendMessage(message);

            this.semaphore.release();
        }
        catch (IOException e) {
            Log.d("CLIENTTHREAD", "ERROR");
            e.printStackTrace();
            this.semaphore.release();
        }
    }

    private String parseHeader(String line) {
        String[] parts = line.split(" ");

        if(parts[1].substring(parts[1].length() - 1).equals("/"))
            return "index.html";

        parts = parts[1].split("/");

        return parts[parts.length - 1];
    }

    private File loadPage(File sdcard, String url) {
        File file = new File(sdcard, url);

        if(file.exists() && !file.isDirectory())
            return file;
        else
            return null;
    }

    private String failPage() {
        return "HTTP/1.1 404 Not Found\n" +
                "Content-Type: text/html\n" +
                "\n" +
                "<html>\n" +
                "<title>Error 404 (Not Found)</title>\n" +
                "<body>\n" +
                "<p><b>404</b> PAGE NOT FOUND!\n" +
                "<p>The requested URL was not found on this server.\n" +
                "</body>\n" +
                "</html>";
    }
}
