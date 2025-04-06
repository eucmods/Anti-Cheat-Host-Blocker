package com.mvpst.bp;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import android.text.TextUtils;
import com.mvpst.bp.cp.BaseLog;




public class HostManager {
    private static final String TAG = "HostManager";
    private static HostManager instance;
    private HashMap<String, String> hostMap;
	private HashMap as;
	
	
    private HostManager() {
        hostMap = new HashMap<String, String>();
    }

    public static HostManager getInstance() {
        if (instance == null) {
            instance = new HostManager();
        }
        return instance;
    }
	
	
	public void loadHosts(Context context) {
        InputStream open;
        try {
            open = context.getAssets().open("host.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
                String trim = readLine.trim();
                if (!TextUtils.isEmpty(trim)) {
                    String[] split = trim.split("\\s+");
                    if (split.length == 2 && !split[0].startsWith("#")) {
                        hostMap.put(split[1].trim(), split[0].trim());
                        Log.i("_HostManager", "loadFile: " + split[1] + " " + split[0]);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("_HostManager", "loadFile: ", e);
        }
       // Log.i("_HostManager", "loadFile: size " + mid.size());
    }

	
	public String mid(String str) {
        return (String) this.as.get(str);
    }
	
	
    public void loadHosts2(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("host.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2 && !parts[0].startsWith("#")) {
                        String ip = parts[0].trim();
                        String domain = parts[1].trim();
                        hostMap.put(domain, ip);
                        Log.i(TAG, "loadHosts: " + domain + " -> " + ip);
                    }
                }
            }
            reader.close();
            inputStream.close();
            Log.i(TAG, "loadHosts: Loaded " + hostMap.size() + " entries");
        } catch (Exception e) {
            Log.e(TAG, "loadHosts: Error loading host.txt", e);
        }
    }

    public boolean isDomainBlocked(String domain) {
        String ip = hostMap.get(domain);
        return ip != null && ip.equals("127.0.0.1");
    }
}
