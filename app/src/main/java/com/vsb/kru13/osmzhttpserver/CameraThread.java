package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
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

public class CameraThread extends Thread {
    private Socket socket;
    private String inputLine, page_file;

    private String SNAPSHOT_URL = "snapshot";
    private String STREAM_URL = "stream";

    public CameraThread(Socket socket) {
        this.socket = socket;
        this.inputLine = this.page_file = "";
    }

    @Override
    public void run() {
        try {
            Log.d("CAMERATHREAD", "Socket Accepted, initializing...");

            OutputStream o = this.socket.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            Log.d("CAMERATHREAD", "Parsing header...");

            while((this.inputLine = in.readLine()) != null && !this.inputLine.isEmpty()) {
                if(this.inputLine.contains("HTTP") && this.page_file.equals("")) {
                    this.page_file = parseHeader(this.inputLine);
                    break;
                }
            }

            Log.d("CAMERATHREAD", "Header parsed successfully, looking for a page...");

            File sdcard = Environment.getExternalStorageDirectory();
            File page;
            if(this.page_file.equals("snapshot") || this.page_file.equals("stream"))
                page = loadPage(sdcard, "refresh_index.html");
            else
                page = loadPage(sdcard, this.page_file);

            if(page != null) {
                FileInputStream file = new FileInputStream(page);
                String contentType;

                if(this.page_file.contains(".html"))
                    contentType = "Content-Type: text/html\n";
                else if(this.page_file.contains(".jpg"))
                    contentType = "Content-Type: image/jpeg\n";
                else
                    contentType = "Content-Type: text/html\n";

                Log.d("CAMERATHREAD", "Loading requested " + this.page_file);


                //nedokazal som rozchodit MJPEG, takze funguje iba zobrazovanie posledneho ulozeneho obrazku
                if(this.page_file.contains(".jpg") && CameraActivity.getSavedBytePicture() != null) {
                    byte[] buffer = CameraActivity.getSavedBytePicture();
                    out.write("HTTP/1.1 200 OK\n" +
                            contentType +
                            "Content-Length:" + buffer.length + "\n" +
                            "\n");

                    o.flush();
                    out.flush();

                    o.write(buffer, 0, buffer.length);
                } else {
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

                    if(this.page_file.equals("stream") && CameraActivity.getSavedBytePicture() != null) {
                        o.flush();
                        out.flush();
                        out.write("--OSMZ_boundary");
                    }
                }
            }
            else {
                Log.d("CAMERATHREAD", "404: Page not found!");
                out.write(failPage());
            }

            o.flush();
            out.flush();

            this.socket.close();
            o.close();
            out.close();
            in.close();

            Log.d("CAMERATHREAD", "Socket Closed");
        }
        catch (IOException e) {
            Log.d("CAMERATHREAD", "ERROR");
            e.printStackTrace();
        }
    }

    private String parseHeader(String line) {
        String[] parts = line.split(" ");

        if(parts[1].substring(parts[1].length() - 1).equals("/"))
            return "refresh_index.html";

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
