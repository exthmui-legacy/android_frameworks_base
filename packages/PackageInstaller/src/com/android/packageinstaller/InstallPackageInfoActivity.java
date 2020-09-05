package com.android.packageinstaller;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.packageinstaller.ui.InstallPackageInfo;
import com.android.packageinstaller.ui.InstallPackageInfoAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InstallPackageInfoActivity extends AppCompatActivity {

    List<InstallPackageInfo> infos = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.install_package_info_layout);
        initView();
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.install_package_info);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initView() {
        RecyclerView recyclerView = findViewById(R.id.install_package_info_container);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String[] titles = {getString(R.string.package_info_package_label), getString(R.string.package_info_package_name),
                getString(R.string.package_info_package_version_name), getString(R.string.package_info_package_first_install_time),
                getString(R.string.package_info_package_last_update_time), getString(R.string.package_info_package_version_code)};

        String key_package_label = getIntent().getStringExtra("key_package_label");
        String key_package_name = getIntent().getStringExtra("key_package_name");
        String key_versionName = getIntent().getStringExtra("key_versionName");
        if (key_versionName == null){
            key_versionName="null";
        }
        long key_package_first_install_time = getIntent().getLongExtra("key_package_first_install_time", 0);
        long key_package_last_update_time = getIntent().getLongExtra("key_package_last_update_time", 0);
        long key_versionCode = getIntent().getLongExtra("key_versionCode", 0);
        String[] summarys = {key_package_label, key_package_name, key_versionName,
                time2String(key_package_first_install_time), time2String(key_package_last_update_time), String.valueOf(key_versionCode)};
        for (int i = 0; i < titles.length; i++) {
            infos.add(new InstallPackageInfo(titles[i], summarys[i]));
        }

        if (key_package_first_install_time==0){
            infos.remove(3);
        }
        if (key_package_last_update_time==0){
            infos.remove(3);
        }
        recyclerView.setAdapter(new InstallPackageInfoAdapter(infos));
    }

    private String time2String(long time) {
        return new SimpleDateFormat("yyyy年M月d日 HH:mm:ss").format(new Date(time));
    }
}
