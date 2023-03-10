package com.god.backgroundmanager;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.god.backgroundmanager.Adapeter.ListAppAdapter;
import com.god.backgroundmanager.Entity.AppInfo;
import com.god.backgroundmanager.Enum.BackStackState;
import com.god.backgroundmanager.Enum.GroupAppType;
import com.god.backgroundmanager.Enum.OrderAppType;
import com.god.backgroundmanager.Enum.SortAppType;
import com.god.backgroundmanager.Facade.AppManagerFacade;
import com.god.backgroundmanager.Permission.PermissionHandler;
import com.god.backgroundmanager.Tasking.TaskingHandler;
import com.god.backgroundmanager.Util.DialogUtils;
import com.god.backgroundmanager.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private AppManagerFacade appManagerFacade;
    private ListAppAdapter crrListListAppAdapter;
    private GroupAppType selectedGroupAppType = GroupAppType.ALL;
    private OrderAppType selectedOrderAppType = OrderAppType.NAME;
    private SortAppType selectedSortAppType = SortAppType.A_TO_Z;
    private MenuItem prevSelectGroupTypeItem;
    private MenuItem prevSelectOrderTypeItem;
    private MenuItem prevSelectSortTypeItem;
    private TextView appTitleLabel;
    private LinearLayout selectOptionBar;
    private Menu optionsMenu;
    private boolean isOpenSearchBar = false;
    private DrawerLayout mainDrawer;
    private final Stack<BackStackState> backStack = new Stack<>();
    private TaskingHandler taskingHandler;
    private PermissionHandler permissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPermission();
        initAppManagerFacade();
        initTaskingHandler();
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLayout(binding);
        initProperty();
        initListAppRecycleView();
        initSearchEvent();
        taskingHandler.execTaskGetAllInstalledApp();
    }

    private void initAppManagerFacade() {
        appManagerFacade = AppManagerFacade.GetInstance(this);
        appManagerFacade.getRootPermission();
    }

    private void initPermission() {
        permissionHandler = new PermissionHandler(this);
        permissionHandler.getPermissions();
    }

    private void initTaskingHandler() {
        taskingHandler = new TaskingHandler(this);
        taskingHandler.setCallbackSetListAppToRecycleView(
                this::setListAppToRecycleView);
    }

    private void initLayout(ActivityMainBinding binding) {
        setSupportActionBar(binding.toolbar);
        mainDrawer = binding.drawerLayout;
        NavigationView navView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mainDrawer,
                binding.toolbar, R.string.navigation_draw_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(item->{
            int id = item.getItemId();
            switch (id){
                case R.id.refesh_list_app:{
                    taskingHandler.execTaskGetAllInstalledApp();
                }
            }
            mainDrawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode,permissions,grantResults);
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
                            }
                    );
                    return true;
                case R.id.freeze:
                    //TODO: make freeze app
                    return true;
                case R.id.turn_off_notification:
                    //TODO: turn off notification
                    return true;
                case R.id.uninstall:
                    taskingHandler.execTaskUninstallApp(selectedAppInfo);
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
        handleBackStack();
    }

    private void handleBackStack() {
        if (mainDrawer.isDrawerOpen(GravityCompat.START)) {
            mainDrawer.closeDrawer(GravityCompat.START);
        } else {
            if (backStack.empty()) {
                super.onBackPressed();
            } else {
                BackStackState prevBackStack = backStack.pop();
                switch (prevBackStack) {
                    case SEARCH:
                        setOpenSearchBar(true);
                        break;
                    case SELECT:
                        crrListListAppAdapter.toggleEnableSelect();
                        break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean isHaveToUpdateListApp = false;

        isHaveToUpdateListApp = isHaveToUpdateListApp || handleMenuFilter(item, id);
        isHaveToUpdateListApp = isHaveToUpdateListApp || handleMenuOrder(item, id);
        isHaveToUpdateListApp = isHaveToUpdateListApp || handleMenuSort(item, id);
        if (isHaveToUpdateListApp) {
            item.setChecked(true);
            taskingHandler.handleTaskSetListAppToRecycleView();
        } else {
            handleMenuOther(id);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initProperty() {
        appTitleLabel = findViewById(R.id.app_name_title);
        selectOptionBar = findViewById(R.id.select_options_bar);
    }

    private void handleMenuOther(int id) {
        switch (id) {
            case R.id.open_search_bar:
                handleOpenSearchBar();
                break;
            case R.id.select_multiple:
                handleSelectOptionClick();
                break;
        }
    }

    private void handleSelectOptionClick() {
        crrListListAppAdapter.toggleEnableSelect();
        if (crrListListAppAdapter.getSelectionState()) {
            selectOptionBar.setVisibility(View.VISIBLE);
            appTitleLabel.setVisibility(View.GONE);
            if (isOpenSearchBar) {
                backStack.push(BackStackState.SELECT);
            }
        } else {
            if (isOpenSearchBar) {
                handleBackStack();
            } else {
                appTitleLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleOpenSearchBar() {
        setOpenSearchBar(true);
        appTitleLabel.setVisibility(View.GONE);
        selectOptionBar.setVisibility(View.GONE);
    }

    private void handleCheckEdMenuFilter(MenuItem item, GroupAppType selectedGroupApp) {
        if (prevSelectGroupTypeItem != null && item != prevSelectGroupTypeItem) {
            prevSelectGroupTypeItem.setChecked(false);
        }
        prevSelectGroupTypeItem = item;
        selectedGroupAppType = selectedGroupApp;
    }

    private void handleCheckEdMenuOrder(MenuItem item, OrderAppType selectedOrderApp) {
        if (prevSelectOrderTypeItem != null && item != prevSelectOrderTypeItem) {
            prevSelectOrderTypeItem.setChecked(false);
        }
        prevSelectOrderTypeItem = item;
        selectedOrderAppType = selectedOrderApp;
    }

    private void handleCheckEdMenuSort(MenuItem item, SortAppType selectedSortApp) {
        if (prevSelectSortTypeItem != null && item != prevSelectSortTypeItem) {
            prevSelectSortTypeItem.setChecked(false);
        }
        prevSelectSortTypeItem = item;
        selectedSortAppType = selectedSortApp;
    }

    private boolean handleMenuFilter(MenuItem item, int id) {
        boolean isHaveToUpdateListApp = false;
        switch (id) {
            case R.id.not_filter_app:
                handleCheckEdMenuFilter(item, GroupAppType.ALL);
                isHaveToUpdateListApp = true;
                break;
            case R.id.filter_system_app:
                handleCheckEdMenuFilter(item, GroupAppType.SYSTEM_APP);
                isHaveToUpdateListApp = true;
                break;
            case R.id.filter_user_app:
                handleCheckEdMenuFilter(item, GroupAppType.USER_APP);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }

    private boolean handleMenuSort(MenuItem item, int id) {
        boolean isHaveToUpdateListApp = false;
        switch (id) {
            case R.id.sort_a_to_z:
                handleCheckEdMenuSort(item, SortAppType.A_TO_Z);
                isHaveToUpdateListApp = true;
                break;
            case R.id.sort_z_to_a:
                handleCheckEdMenuSort(item, SortAppType.Z_To_A);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }

    private boolean handleMenuOrder(MenuItem item, int id) {
        boolean isHaveToUpdateListApp = false;
        switch (id) {
            case R.id.order_by_app_name:
                handleCheckEdMenuOrder(item, OrderAppType.NAME);
                isHaveToUpdateListApp = true;
                break;
            case R.id.order_by_package_name:
                handleCheckEdMenuOrder(item, OrderAppType.PACKAGE_NAME);
                isHaveToUpdateListApp = true;
                break;
        }
        return isHaveToUpdateListApp;
    }



    private void initListAppRecycleView() {
        RecyclerView listAppRecycleView = findViewById(R.id.list_app);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        listAppRecycleView.setLayoutManager(layoutManager);
        crrListListAppAdapter = new ListAppAdapter(new ArrayList<>());
        crrListListAppAdapter.setOnTouchEvent((listServiceLayout,
                                               appInfo)
                ->taskingHandler.execTaskHandleGetListService(listServiceLayout, appInfo.packageName));
        listAppRecycleView.setAdapter(crrListListAppAdapter);
        registerForContextMenu(listAppRecycleView);
        listAppRecycleView.setVerticalScrollBarEnabled(true);
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
        (findViewById(R.id.close_search_bar_btn)).setOnClickListener((v) -> {
            setOpenSearchBar(false);
            appTitleLabel.setVisibility(View.VISIBLE);
        });
        SearchView searchApp = findViewById(R.id.search_app);
        searchApp.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                taskingHandler.execTaskForSearchApp(newText);
                return false;
            }
        });
    }

    private void setOpenSearchBar(boolean isOpenSearchBar) {
        this.isOpenSearchBar = isOpenSearchBar;
        if (optionsMenu != null) {
            for (int i = 0; i < optionsMenu.size(); i++) {
                MenuItem crrGroup = optionsMenu.getItem(i);
                if (crrGroup.getItemId() != R.id.menu_other_options) {
                    crrGroup.setVisible(!isOpenSearchBar);
                }
            }
        }
        findViewById(R.id.search_bar).setVisibility(isOpenSearchBar ? View.VISIBLE : View.GONE);
        if (isOpenSearchBar) {
            SearchView searchView = findViewById(R.id.search_app);
            searchView.requestFocus();
            searchView.setIconified(false);
            searchView.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        }
    }
}