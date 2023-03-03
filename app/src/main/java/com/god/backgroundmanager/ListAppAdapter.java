package com.god.backgroundmanager;

import android.content.Context;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAppAdapter extends RecyclerView.Adapter<ListAppAdapter.AppItemHolder>
        implements View.OnCreateContextMenuListener{
    private List<AppInfo> listApp;
    private AppInfo selectedAppInfo;
    private final Context context;
    public ListAppAdapter(Context context, List<AppInfo> listApp){
        this.listApp = listApp;
        this.context = context;
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
        holder.itemView.setOnCreateContextMenuListener(this);
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
        public AppItemHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appNameLabel = itemView.findViewById(R.id.app_name);
            appPackageLabel = itemView.findViewById(R.id.app_package);
            appNameLabel.setSelected(true);
            appNameLabel.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            appNameLabel.setMarqueeRepeatLimit(-1);
            appPackageLabel.setSelected(true);
            appPackageLabel.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            appPackageLabel.setMarqueeRepeatLimit(-1);
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
