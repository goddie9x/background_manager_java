package com.god.backgroundmanager.Facade;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.god.backgroundmanager.Entity.AppInfo;
import com.god.backgroundmanager.Util.DialogUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppManagerFacade {
    public interface EventVoid{
        void callback();
    }
    private final PackageManager packageManager;
    private final ActivityManager activityManager;
    private final AppCompatActivity activity;
    private static AppManagerFacade instance;
    public static final int SDK_VERSION = android.os.Build.VERSION.SDK_INT;
    private static final String TAG = "app manager facade";
    public static boolean hasRootPermission=false;
    private AppManagerFacade(AppCompatActivity activity){
        this.activity = activity;
        this.packageManager = this.activity.getPackageManager();
        this.activityManager = (ActivityManager) this.activity.getSystemService(Context.ACTIVITY_SERVICE);
    }
    public static AppManagerFacade GetInstance(AppCompatActivity activity){
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
            AppInfo appInfo = new AppInfo(appIcon,
                    appName, packageName,(applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            appList.add(appInfo);
        }
        return appList;
    }
    public List<ActivityManager.RunningAppProcessInfo> GetAllRunningApp(){
        return activityManager.getRunningAppProcesses();
    }
    public void getRootPermission(){
        if(!hasRootAccess()){
            try {
                Runtime.getRuntime().exec("su");
            }
            catch (IOException e){
                Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
    }
    public boolean hasRootAccess() {
        try {
            java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime()
                    .exec(new String[]{"/system/bin/su","-c","cd / && ls"})
                    .getInputStream()).useDelimiter("\\A");
            hasRootPermission = !(s.hasNext() ? s.next() : "").equals("");
            return AppManagerFacade.hasRootPermission;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public void uninstallAppWithRoot(String packageName){
        executeCommandWithSuShell("pm uninstall "+packageName,
                "Uninstall "+packageName+" success",
                "Uninstall "+packageName+" failed");
    }
    public void uninstallApp(AppInfo appInfo){
        if(appInfo.isSystemApp){
            if(hasRootPermission) {
                uninstallAppWithRoot(appInfo.packageName);
            }
            else{
                Toast.makeText(activity,
                        "You cannot uninstall system app without root",Toast.LENGTH_LONG);
            }
        }
        else{
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + appInfo.packageName));
            activity.startActivity(intent);
        }
    }
    public void forceStopAppWithRootPermission(String packageName){
        executeCommandWithSuShell("am force-stop "+packageName,
                "Kill "+packageName+" success",
                "Kill "+packageName+" failed");
    }
    public void forceStopApp(String packageName) {
        if(hasRootPermission){
            forceStopAppWithRootPermission(packageName);
        }
        else{
            //since android 10 (sdk 19) we cannot force stop app, then we open setting of this app
            //so we let users do it by their own
            if(SDK_VERSION<29){
                activityManager.killBackgroundProcesses(packageName);
            }
            else{
                DialogUtils.showAlertDialog(activity,"Manual",
                        "Your android version not support or you do not have root permission" +
                                "\n must force stop app as manual",(d,w)->{
                            openAppSetting(packageName);
                        });
            }
        }
    }
    public void executeCommandWithSuShell(String command){
        try {
            Log.i(TAG, "exec command");
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                os.writeBytes("adb shell" + "\n");
                os.flush();
                os.writeBytes(command + "\n");
                os.flush();
                Toast.makeText (activity,"Done ",Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText (activity,"Failed",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void executeCommandWithSuShell(String command,String textSuccess,String textFail){
        try {
            Log.i(TAG, "exec executeCommandWithSuShell");
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                os.writeBytes("adb shell" + "\n");
                os.flush();
                os.writeBytes(command + "\n");
                os.flush();
                Toast.makeText (activity,textSuccess,Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText (activity,textFail,Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void executeCommandWithSuShell(String command,
                                          EventVoid onSuccess,
                                          EventVoid onFailed){
        try {
            Log.i(TAG, "exec command");
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                os.writeBytes("adb shell" + "\n");
                os.flush();
                os.writeBytes(command + "\n");
                os.flush();
                onSuccess.callback();
            } catch (IOException e) {
                onFailed.callback();
                Toast.makeText (activity,"Failed",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            onFailed.callback();
            e.printStackTrace();
        }
    }
    public void openAppSetting(String packageName){
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        activity.startActivity(intent);
    }
    public AppCompatActivity getActivity() {
        return activity;
    }
    public List<ActivityManager.RunningServiceInfo> getRunningServices(){
        return activityManager
                .getRunningServices(Integer.MAX_VALUE);
    }
    public ActivityInfo[]getServices(String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_ACTIVITIES);
            return packageInfo.activities;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
