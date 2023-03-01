package com.god.backgroundmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAppAdapter extends RecyclerView.Adapter<ListAppAdapter.AppItemHolder>{
    private final List<AppInfo> listApp;
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
        return new AppItemHolder(itemView);
    }

    public void onBindViewHolder(@NonNull ListAppAdapter.AppItemHolder holder, int position) {
        AppInfo crrAppInfo = listApp.get(position);
        holder.appNameLabel.setText(crrAppInfo.appName);
        holder.appPackageLabel.setText(crrAppInfo.packageName);
        holder.appIcon.setImageDrawable(crrAppInfo.appIcon);
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
        }
    }
}
