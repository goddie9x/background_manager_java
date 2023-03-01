package com.god.backgroundmanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AppManagerFacade {
    private PackageManager packageManager;
    private ActivityManager activityManager;
    private AppCompatActivity activity;
    private static AppManagerFacade instance;
    private AppManagerFacade(AppCompatActivity activity){
        this.activity = activity;
        this.packageManager = this.activity.getPackageManager();
        activityManager = (ActivityManager) this.activity.getSystemService(Context.ACTIVITY_SERVICE);
    }
    public static AppManagerFacade GetIntance(AppCompatActivity activity){
        if(instance==null||instance.activity!=activity){
            instance = new AppManagerFacade((activity));
        }
        return instance;
    }
    public List<AppInfo> GetAllInstalledApp(){
        List<AppInfo> appList = new ArrayList<>();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : installedApplications) {
            Drawable appIcon = applicationInfo.loadIcon(packageManager);
            String appName = applicationInfo.loadLabel(packageManager).toString();
            String packageName = applicationInfo.packageName;
            AppInfo appInfo = new AppInfo(appIcon, appName, packageName);
            appList.add(appInfo);
        }
        return appList;
    }
    public List<ActivityManager.RunningAppProcessInfo> GetAllRunningApp(){
        return activityManager.getRunningAppProcesses();
    }
    public void forceStopApp(String packageName) {
        activityManager.killBackgroundProcesses(packageName);
    }
    public AppCompatActivity getActivity() {
        return activity;
    }
    public List<ActivityInfo> getServices(String packageName) {
        List<ActivityInfo> services = new ArrayList<>();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null) {
                for (ActivityInfo activityInfo : activities) {
                    if (activityInfo.exported && activityInfo.permission == null) {
                        services.add(activityInfo);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return services;
    }
}
