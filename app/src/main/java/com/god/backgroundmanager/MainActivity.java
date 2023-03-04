package com.god.backgroundmanager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.god.backgroundmanager.Adapeter.ListAppAdapter;
import com.god.backgroundmanager.Entity.AppInfo;
import com.god.backgroundmanager.Facade.AppManagerFacade;
import com.god.backgroundmanager.Util.AsyncTaskBuilder;
import com.god.backgroundmanager.Util.DialogUtils;
import com.god.backgroundmanager.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private AppManagerFacade appManagerFacade;
    private ListAppAdapter crrListListAppAdapter;
    private List<AppInfo> listApp;
    private AsyncTaskBuilder<Void,Void,Void> taskGetAllInstalledApp;
    private AsyncTaskBuilder<String,Void,Void> taskSearchApp;
    private static final int PERMISSION_QUERY_ALL_PACKAGES_CODE = 69;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermissions();
        appManagerFacade = AppManagerFacade.GetInstance(this);
        appManagerFacade.getRootPermission();
        com.god.backgroundmanager.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        initListAppRecycleView();
        initTasks();
        initSearchEvent();
        taskGetAllInstalledApp.execute();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_QUERY_ALL_PACKAGES_CODE) {
            if (grantResults.length < 1
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                DialogUtils.showAlertDialog(this,
                        "Permission require warning",
                        "You should provide permission to get full power",
                            (dialog,which)-> getQueryAllPackagePermission(),
                            (dialog,which)->{}
                        );
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AppInfo selectedAppInfo = crrListListAppAdapter.getSelectedAppInfo();
        if(selectedAppInfo!=null) {
            switch (item.getItemId()) {
                case R.id.force_stop:
                    DialogUtils.showAlertDialog(this,
                            "Confirmation",
                            "Do you want to force stop app: "+selectedAppInfo.packageName,
                            (dialog,which)->{
                                Toast.makeText(this,
                                        "Force stopping "+selectedAppInfo.packageName,
                                        Toast.LENGTH_SHORT).show();
                                appManagerFacade.forceStopApp(selectedAppInfo.packageName);
                            },
                            (dialog,which)->{}
                    );
                    return true;
                case R.id.freeze:
                    return true;
                case R.id.turn_off_notification:
                    return true;
                case R.id.uninstall:
                    return true;
                case R.id.open_in_setting:
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
    private void getPermissions(){
        getQueryAllPackagePermission();
    }
    private void getQueryAllPackagePermission(){
        if (checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestPermissions(new String[]{Manifest.permission.QUERY_ALL_PACKAGES},
                        PERMISSION_QUERY_ALL_PACKAGES_CODE);
            }
        }
    }
    private void initTasks(){
        initTaskGetAllInstalledApp();
        initTaskForSearchApp();
    }
    private void initListAppRecycleView(){
        RecyclerView listAppRecycleView = findViewById(R.id.list_app);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        listAppRecycleView.setLayoutManager(layoutManager);
        listAppRecycleView.setHasFixedSize(true);
        listAppRecycleView.setScrollbarFadingEnabled(false);
        listApp = new ArrayList<>();
        crrListListAppAdapter = new ListAppAdapter(listApp);
        crrListListAppAdapter.setOnTouchEvent((listServiceLayout,appInfo)-> execTaskHandleGetListService(listServiceLayout,appInfo.packageName));
        listAppRecycleView.setAdapter(crrListListAppAdapter);
        registerForContextMenu(listAppRecycleView);
    }
    private void execTaskHandleGetListService(
            LinearLayout listServiceLayout,
            String crrPackageName
    ){
        if(listServiceLayout!=null){
            AsyncTaskBuilder<String, Void, Void> taskHandleGetListService = new AsyncTaskBuilder<>();
            taskHandleGetListService.setDoInBackgroundFunc(packageName->{
                ActivityInfo[] listService = appManagerFacade
                        .getServices((String) packageName[0]);
                if(listService!=null){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(()-> {
                        listServiceLayout.removeAllViews();

                        for (ActivityInfo crrService: listService) {
                            CheckBox crrServiceCheckBox= new CheckBox(this);
                            crrServiceCheckBox.setText(crrService.processName);
                            crrServiceCheckBox.setChecked(crrService.enabled);
                            crrServiceCheckBox.setOnCheckedChangeListener((v,isChecked)->{

                            });
                            listServiceLayout.addView(crrServiceCheckBox);
                        }
                    });
                }
                return null;
            });
            taskHandleGetListService.execute(crrPackageName);
        }
    }
    private void initTaskForSearchApp(){
        taskSearchApp = new AsyncTaskBuilder<>();
        taskSearchApp.setDoInBackgroundFunc(queries->{
            String queryText = (String)queries[0];

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(()-> {
                List<AppInfo> newListApp = listApp;
                if(!queryText.isEmpty()){
                    newListApp =listApp.stream().filter(appInfo -> appInfo
                            .packageName.contains(queryText)||appInfo.appName.contains(queryText))
                            .collect(Collectors.toList());
                }
                crrListListAppAdapter.setListApp(newListApp);
            });
            return null;
        });
    }
    private void initTaskGetAllInstalledApp(){
        taskGetAllInstalledApp = new AsyncTaskBuilder<>();
        ProgressDialog progressDialog = new ProgressDialog(this);

        taskGetAllInstalledApp.setDoInBackgroundFunc(ts->{
            listApp  = appManagerFacade.GetAllInstalledApp();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(()-> crrListListAppAdapter.setListApp(listApp));
            return null;
        });
        taskGetAllInstalledApp.setOnPreExecuteFunc(()->{
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(()-> {
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            });
        });
        taskGetAllInstalledApp.setOnPostExecuteFunc(
                result->{
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(()->{
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    });
                }
        );
    }
    private void initSearchEvent(){
        SearchView searchApp = findViewById(R.id.search_app);
        searchApp.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                initTaskForSearchApp();
                taskSearchApp.execute(newText);
                return false;
            }
        });
    }

}