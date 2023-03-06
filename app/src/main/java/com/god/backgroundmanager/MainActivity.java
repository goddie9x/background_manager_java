package com.god.backgroundmanager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.god.backgroundmanager.Adapeter.ListAppAdapter;
import com.god.backgroundmanager.Entity.AppInfo;
import com.god.backgroundmanager.Enum.GroupAppType;
import com.god.backgroundmanager.Enum.OrderAppType;
import com.god.backgroundmanager.Enum.SortAppType;
import com.god.backgroundmanager.Facade.AppManagerFacade;
import com.god.backgroundmanager.Util.AsyncTaskBuilder;
import com.god.backgroundmanager.Util.DialogUtils;
import com.god.backgroundmanager.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private AppManagerFacade appManagerFacade;
    private ListAppAdapter crrListListAppAdapter;
    private List<AppInfo> listApp;
    private GroupAppType selectedGroupAppType = GroupAppType.ALL;
    private OrderAppType selectedOrderAppType = OrderAppType.NAME;
    private SortAppType selectedSortAppType = SortAppType.A_TO_Z;
    private static final int PERMISSION_QUERY_ALL_PACKAGES_CODE = 69;
    private MenuItem prevSelectGroupTypeItem;
    private MenuItem prevSelectOrderTypeItem;
    private MenuItem prevSelectSortTypeItem;
    private Menu optionsMenu;
    private boolean isOpenSearchBar = false;

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
        initSearchEvent();
        execTaskGetAllInstalledApp();
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
                        (dialog, which) -> getQueryAllPackagePermission(),
                        (dialog, which) -> {
                        }
                );
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AppInfo selectedAppInfo = crrListListAppAdapter.getSelectedAppInfo();
        if (selectedAppInfo != null) {
            switch (item.getItemId()) {
                case R.id.force_stop:
                    DialogUtils.showAlertDialog(this,
                            "Confirmation",
                            "Do you want to force stop app: " + selectedAppInfo.packageName,
                            (dialog, which) -> {
                                Toast.makeText(this,
                                        "Force stopping " + selectedAppInfo.packageName,
                                        Toast.LENGTH_SHORT).show();
                                appManagerFacade.forceStopApp(selectedAppInfo.packageName);
                            },
                            (dialog, which) -> {
                            }
                    );
                    return true;
                case R.id.freeze:
                    return true;
                case R.id.turn_off_notification:
                    return true;
                case R.id.uninstall:
                    execTaskUninstallApp(selectedAppInfo);
                    return true;
                case R.id.open_in_setting:
                    appManagerFacade.openAppSetting(selectedAppInfo.packageName);
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        optionsMenu = menu;
        return true;
    }
    @Override
    public void onBackPressed() {

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean isHaveToUpdateListApp = false;

        isHaveToUpdateListApp = isHaveToUpdateListApp||handleMenuFilter(item,id);
        isHaveToUpdateListApp = isHaveToUpdateListApp||handleMenuOrder(item,id);
        isHaveToUpdateListApp = isHaveToUpdateListApp||handleMenuSort(item,id);
        switch (id) {
            case R.id.open_search_bar:
                setOpenSearchBar(true);
                break;
            case R.id.select_multiple:
                crrListListAppAdapter.toggleEnableSelect();
                break;
        }
        item.setChecked(true);
        if(isHaveToUpdateListApp){
            handleTaskSetListAppToRecycleView();
        }
        return super.onOptionsItemSelected(item);
    }
    private void handleCheckEdMenuFilter(MenuItem item,GroupAppType selectedGroupApp){
        if (prevSelectGroupTypeItem != null && item != prevSelectGroupTypeItem) {
            prevSelectGroupTypeItem.setChecked(false);
        }
        prevSelectGroupTypeItem = item;
        selectedGroupAppType = selectedGroupApp;
    }
    private void handleCheckEdMenuOrder(MenuItem item,OrderAppType selectedOrderApp){
        if (prevSelectOrderTypeItem != null && item != prevSelectOrderTypeItem) {
            prevSelectOrderTypeItem.setChecked(false);
        }
        prevSelectGroupTypeItem = item;
        selectedOrderAppType = selectedOrderApp;
    }
    private void handleCheckEdMenuSort(MenuItem item,SortAppType selectedSortApp){
        if (prevSelectSortTypeItem != null && item != prevSelectSortTypeItem) {
            prevSelectSortTypeItem.setChecked(false);
        }
        prevSelectSortTypeItem = item;
        selectedSortAppType = selectedSortApp;
    }
    private boolean handleMenuFilter(MenuItem item,int id){
        boolean isHaveToUpdateListApp = false;
        switch (id){
            case R.id.not_filter_app:
                handleCheckEdMenuFilter(item,GroupAppType.ALL);
                isHaveToUpdateListApp = true;
                break;
            case R.id.filter_system_app:
                handleCheckEdMenuFilter(item,GroupAppType.SYSTEM_APP);
                isHaveToUpdateListApp = true;
                break;
            case R.id.filter_user_app:
                handleCheckEdMenuFilter(item,GroupAppType.USER_APP);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }

    private boolean handleMenuSort(MenuItem item, int id){
        boolean isHaveToUpdateListApp = false;
        switch (id){
            case R.id.sort_a_to_z:
                handleCheckEdMenuSort(item,SortAppType.A_TO_Z);
                isHaveToUpdateListApp = true;
                break;
            case R.id.sort_z_to_a:
                handleCheckEdMenuSort(item,SortAppType.Z_To_A);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }
    private boolean handleMenuOrder(MenuItem item,int id){
        boolean isHaveToUpdateListApp = false;
        switch (id){
            case R.id.order_by_app_name:
                handleCheckEdMenuOrder(item,OrderAppType.NAME);
                isHaveToUpdateListApp = true;
                break;
            case R.id.order_by_package_name:
                handleCheckEdMenuOrder(item,OrderAppType.PACKAGE_NAME);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }
    private void getPermissions() {
        getQueryAllPackagePermission();
        getManagerNotificationPermission();
    }

    private void getManagerNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 1);
            }
        }
    }

    private void getQueryAllPackagePermission() {
        if (checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestPermissions(new String[]{Manifest.permission.QUERY_ALL_PACKAGES},
                        PERMISSION_QUERY_ALL_PACKAGES_CODE);
            }
        }
    }

    private void initListAppRecycleView() {
        RecyclerView listAppRecycleView = findViewById(R.id.list_app);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        listAppRecycleView.setLayoutManager(layoutManager);
        listAppRecycleView.setHasFixedSize(true);
        listAppRecycleView.setScrollbarFadingEnabled(false);
        listApp = new ArrayList<>();
        crrListListAppAdapter = new ListAppAdapter(listApp);
        crrListListAppAdapter.setOnTouchEvent((listServiceLayout,
                                               appInfo)
                -> execTaskHandleGetListService(listServiceLayout, appInfo.packageName));
        listAppRecycleView.setAdapter(crrListListAppAdapter);
        registerForContextMenu(listAppRecycleView);
    }

    private void execTaskHandleGetListService(
            LinearLayout listServiceLayout,
            String crrPackageName
    ) {
        if (listServiceLayout != null) {
            AsyncTaskBuilder<String, Void, Void> taskHandleGetListService = new AsyncTaskBuilder<>();
            taskHandleGetListService.setDoInBackgroundFunc(packageName -> {
                ActivityInfo[] listService = appManagerFacade
                        .getServices((String) packageName[0]);
                if (listService != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        listServiceLayout.removeAllViews();

                        for (ActivityInfo crrService : listService) {
                            CheckBox crrServiceCheckBox = new CheckBox(this);
                            crrServiceCheckBox.setText(crrService.processName);
                            crrServiceCheckBox.setChecked(crrService.enabled);
                            crrServiceCheckBox.setOnCheckedChangeListener((v, isChecked) -> {

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

    private void handleTaskSetListAppToRecycleView() {
        AsyncTaskBuilder<Void, Void, Void> taskSetListAppToRecycleView = new AsyncTaskBuilder<>();
        taskSetListAppToRecycleView.setDoInBackgroundFunc(an -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                setListAppToRecycleView(listApp);
            });
            return null;
        });
        taskSetListAppToRecycleView.execute();
    }

    private void execTaskForSearchApp(String queryText) {
        AsyncTaskBuilder<Void, Void, Void> taskSearchApp = new AsyncTaskBuilder<>();
        taskSearchApp.setDoInBackgroundFunc((val) -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                List<AppInfo> newListApp = new ArrayList<>();
                if (!queryText.isEmpty()) {
                    newListApp = listApp.stream().filter(appInfo -> appInfo
                                    .packageName.contains(queryText) || appInfo.appName.contains(queryText))
                            .collect(Collectors.toList());
                } else {
                    newListApp = listApp;
                }
                setListAppToRecycleView(newListApp);
            });
            return null;
        });
        taskSearchApp.execute();
    }

    private void execTaskUninstallApp(AppInfo appInfo) {
        AsyncTaskBuilder<Void, Void, Void> taskUninstallApp = new AsyncTaskBuilder<>();
        ProgressDialog progressDialog = new ProgressDialog(this);

        taskUninstallApp.setDoInBackgroundFunc(ts -> {
            appManagerFacade.uninstallApp(appInfo);
            return null;
        });

        taskUninstallApp.execute();
    }

    private void execTaskGetAllInstalledApp() {
        AsyncTaskBuilder<Void, Void, Void> taskGetAllInstalledApp = new AsyncTaskBuilder<>();
        ProgressDialog progressDialog = new ProgressDialog(this);

        taskGetAllInstalledApp.setDoInBackgroundFunc(ts -> {
            listApp = appManagerFacade.GetAllInstalledApp();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> setListAppToRecycleView(listApp));
            return null;
        });
        taskGetAllInstalledApp.setOnPreExecuteFunc(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            });
        });
        taskGetAllInstalledApp.setOnPostExecuteFunc(
                result -> {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    });
                }
        );
        taskGetAllInstalledApp.execute();
    }

    private void setListAppToRecycleView(List<AppInfo> crrListApp) {
        Stream<AppInfo> listAppStream = crrListApp.stream();
        listAppStream = handleFilterListAppStream(listAppStream);
        listAppStream = handleSortListAppStream(listAppStream);
        crrListApp = listAppStream.collect(Collectors.toList());
        crrListListAppAdapter.setListApp(crrListApp);
    }

    private Stream handleSortListAppStream(Stream<AppInfo> listAppStream) {
        switch (selectedSortAppType) {
            case Z_To_A:
                return handleSortZtoAListAppStream(listAppStream);
            default:
                return handleSortAToZListAppStream(listAppStream);
        }
    }

    private Stream handleSortZtoAListAppStream(Stream<AppInfo> listAppStream) {
        switch (selectedOrderAppType) {
            case PACKAGE_NAME:
                return listAppStream.sorted((appInfoPrev, appInfo)
                        -> appInfo.packageName.compareToIgnoreCase(appInfoPrev.packageName));
            case NAME:
            default:
                return listAppStream.sorted((appInfoPrev, appInfo)
                        -> appInfo.appName.compareToIgnoreCase(appInfoPrev.appName));
        }
    }

    private Stream handleSortAToZListAppStream(Stream<AppInfo> listAppStream) {
        switch (selectedOrderAppType) {
            case PACKAGE_NAME:
                return listAppStream.sorted((appInfoPrev, appInfo)
                        -> appInfoPrev.packageName.compareToIgnoreCase(appInfo.packageName));
            case NAME:
            default:
                return listAppStream.sorted((appInfoPrev, appInfo)
                        -> appInfoPrev.appName.compareToIgnoreCase(appInfo.appName));
        }
    }

    private Stream handleFilterListAppStream(Stream<AppInfo> listAppStream) {
        switch (selectedGroupAppType) {
            case SYSTEM_APP:
                return listAppStream.filter(appInfo -> appInfo.isSystemApp);
            case USER_APP:
                return listAppStream.filter(appInfo -> !appInfo.isSystemApp);
            default:
                return listAppStream;
        }
    }

    private void initSearchEvent() {
        ((ImageButton)findViewById(R.id.close_search_bar_btn)).setOnClickListener((v)->{
            setOpenSearchBar(false);
        });
        SearchView searchApp = findViewById(R.id.search_app);
        searchApp.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                execTaskForSearchApp(newText);
                return false;
            }
        });
    }

    private void setOpenSearchBar(boolean isOpenSearchBar) {
        this.isOpenSearchBar = isOpenSearchBar;
        if (optionsMenu != null) {
            for (int i = 0; i < optionsMenu.size(); i++) {
                MenuItem crrGroup = optionsMenu.getItem(i);
                if(crrGroup.getItemId()!=R.id.menu_other_options){
                    crrGroup.setVisible(!isOpenSearchBar);
                }
            }
        }
        findViewById(R.id.app_name_title).setVisibility(isOpenSearchBar? View.GONE:View.VISIBLE);
        findViewById(R.id.search_bar).setVisibility(isOpenSearchBar? View.VISIBLE:View.GONE);
        if(isOpenSearchBar){
            SearchView searchView = findViewById(R.id.search_app);
            searchView.requestFocus();
            searchView.setIconified(false);
            searchView.postDelayed(()->{
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            },200);
        }
    }
}