package com.android.packageinstaller.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.packageinstaller.R;

import java.util.List;

public class InstallPackageInfoAdapter extends RecyclerView.Adapter<InstallPackageInfoAdapter.ViewHolder> {

    List<InstallPackageInfo> mInstallPackageInfos;

    public InstallPackageInfoAdapter(List<InstallPackageInfo> installPackageInfos) {
        this.mInstallPackageInfos = installPackageInfos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.install_package_info_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InstallPackageInfo installPackageInfo = mInstallPackageInfos.get(position);
        holder.mTitle.setText(installPackageInfo.getTitle());
        holder.mSummary.setText(installPackageInfo.getSummary());
    }

    @Override
    public int getItemCount() {
        return mInstallPackageInfos == null ? 0 : mInstallPackageInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTitle;
        TextView mSummary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.info_title);
            mSummary = itemView.findViewById(R.id.info_summary);
        }
    }
}
