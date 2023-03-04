package com.god.backgroundmanager.Adapeter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.god.backgroundmanager.Entity.AppInfo;
import com.god.backgroundmanager.R;

import java.util.List;

public class ListAppAdapter extends RecyclerView.Adapter<ListAppAdapter.AppItemHolder>
        implements View.OnCreateContextMenuListener{
    private List<AppInfo> listApp;
    private AppInfo selectedAppInfo;

    public interface TouchItemEvent{
        void onTouch(LinearLayout view,AppInfo appInfo);
    }
    private TouchItemEvent onTouchEvent;

    public void setOnTouchEvent(TouchItemEvent onTouchEvent) {
        this.onTouchEvent = onTouchEvent;
    }

    public ListAppAdapter(List<AppInfo> listApp){
        this.listApp = listApp;
    }
    @NonNull
    @Override
    public AppItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_app_info_item,
                parent,
                false
                );
        AppItemHolder holder = new AppItemHolder(itemView);
        itemView.setTag(holder);
        return holder ;
    }
    @Override
    public void onBindViewHolder(@NonNull ListAppAdapter.AppItemHolder holder, int position) {
        AppInfo crrAppInfo = listApp.get(position);
        holder.appNameLabel.setText(crrAppInfo.appName);
        holder.appPackageLabel.setText(crrAppInfo.packageName);
        holder.appIcon.setImageDrawable(crrAppInfo.appIcon);
        if(!crrAppInfo.isSystemApp){
            holder.systemLabel.setText("User");
            holder.systemLabel.setTextColor(Color.GREEN);
        }
        holder.itemView.setOnCreateContextMenuListener(this);
        holder.itemView.setOnClickListener(v -> {
            if(onTouchEvent!=null){
                onTouchEvent.onTouch(holder.listServiceLayout,crrAppInfo);
            }
        holder.setShowSevices(!holder.isShowSevices);
        });
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = new MenuInflater(v.getContext());
        inflater.inflate(R.menu.menu_context_app_item, menu);
        AppItemHolder holder = (AppItemHolder)v.getTag();
        if(holder!=null){
            int indexItemSelected =holder.getBindingAdapterPosition();
            selectedAppInfo = (indexItemSelected<listApp.size())? listApp.get(indexItemSelected):null;
        }
    }
    @Override
    public int getItemCount() {
        return listApp.size();
    }
    public static class AppItemHolder extends RecyclerView.ViewHolder{
        private final ImageView appIcon;
        private final TextView appNameLabel;
        private final TextView appPackageLabel;
        private final TextView systemLabel;
        private final LinearLayout listServiceLayout;
        private boolean isShowSevices = false;
        private void setShowSevices(boolean isShowSevices){
            this.isShowSevices = isShowSevices;
            listServiceLayout.setVisibility(this.isShowSevices?View.VISIBLE:View.GONE);
        }
        public AppItemHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            systemLabel = itemView.findViewById(R.id.system_label);
            appNameLabel = itemView.findViewById(R.id.app_name);
            appPackageLabel = itemView.findViewById(R.id.app_package);
            appNameLabel.setSelected(true);
            appNameLabel.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            appNameLabel.setMarqueeRepeatLimit(-1);
            appPackageLabel.setSelected(true);
            appPackageLabel.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            appPackageLabel.setMarqueeRepeatLimit(-1);
            listServiceLayout = itemView.findViewById(R.id.list_service);
        }
    }
    public AppInfo getSelectedAppInfo(){
        return selectedAppInfo;
    }
    public void clearSelectedAppInfo(){
        selectedAppInfo = null;
    }
    public void setListApp(List<AppInfo> listApp){
        this.listApp = listApp;
        notifyDataSetChanged();
    }
}
