package com.god.backgroundmanager.Entity;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public Drawable appIcon;
    public String appName;
    public String packageName;
    public boolean isSystemApp = false;
    public AppInfo(Drawable appIcon,String appName,String packageName){
        this.appIcon = appIcon;
        this.appName = appName;
        this.packageName = packageName;
    }
    public AppInfo(Drawable appIcon,String appName,String packageName,boolean isSystemApp){
        this.appIcon = appIcon;
        this.appName = appName;
        this.packageName = packageName;
        this.isSystemApp = isSystemApp;
    }
}
