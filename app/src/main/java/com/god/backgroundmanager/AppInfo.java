package com.god.backgroundmanager;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public Drawable appIcon;
    public String appName;
    public String packageName;
    public AppInfo(Drawable appIcon,String appName,String packageName){
        this.appIcon = appIcon;
        this.appName = appName;
        this.packageName = packageName;
    }
}
