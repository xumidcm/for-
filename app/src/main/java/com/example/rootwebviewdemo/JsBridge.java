package com.example.rootwebviewdemo;

import android.webkit.JavascriptInterface;

import org.json.JSONArray;

public class JsBridge {

    @JavascriptInterface
    public String root_cmd(String mycmd) {
        RootShellExecutor.CommandResult result = RootShellExecutor.execute(mycmd);
        JSONArray data = new JSONArray();
        data.put(result.stdout);
        data.put(result.stderr);
        data.put(result.statusCode);
        return data.toString();
    }
}
