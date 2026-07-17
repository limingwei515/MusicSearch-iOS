package com.linfeng.music.view.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.databinding.ActivitySettingsBinding;
import com.linfeng.music.view.base.BaseActivity;
import com.linfeng.music.R;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.linfeng.music.utils.PreferencesManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity<ActivitySettingsBinding> {
    private static final int REQUEST_CODE_CHOOSE_FOLDER = 1001;
    private String downloadPath;

    @Override
    protected ActivitySettingsBinding initViewBinding() {
        return ActivitySettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {
        ImmersionBar.with(this)
                .titleBar(binding.appbar)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.android_background_color)
                .navigationBarDarkIcon(!isNightMode())
                .fitsSystemWindows(true)
                .init();
    }

    @Override
    protected void initActivity() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        // 读取保存的音质设置
        String quality = PreferencesManager.getInstance(this).getString("play_quality", "lossless");
        if (quality.equals("lossless")) {
            binding.radioLossless.setChecked(true);
        } else if (quality.equals("high")) {
            binding.radioHigh.setChecked(true);
        } else if (quality.equals("standard")) {
            binding.radioStandard.setChecked(true);
        } else if (quality.equals("fluent")) {
            binding.radioFluent.setChecked(true);
        }
        binding.radioQuality.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String value = "lossless";
                if (checkedId == binding.radioHigh.getId()) {
                    value = "high";
                } else if (checkedId == binding.radioStandard.getId()) {
                    value = "standard";
                } else if (checkedId == binding.radioFluent.getId()) {
                    value = "fluent";
                }
                PreferencesManager.getInstance(SettingsActivity.this).putString("play_quality", value);
            }
        });

        // 下载路径相关
        TextView textDownloadPath = findViewById(R.id.text_download_path);
        Button btnChooseDownloadPath = findViewById(R.id.btn_choose_download_path);
        downloadPath = PreferencesManager.getInstance(this).getString("download_path", "");
        if (downloadPath.isEmpty()) {
            // 首次使用，重置为新默认路径
            downloadPath = getDefaultDownloadPath();
            PreferencesManager.getInstance(this).putString("download_path", downloadPath);
        }
        textDownloadPath.setText(downloadPath);
        btnChooseDownloadPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE_CHOOSE_FOLDER);
            }
        });

        // 歌词重试次数设置
        int retryCount = PreferencesManager.getInstance(this).getInt("lyric_retry_count", 3);
        if (retryCount == 1) {
            binding.radioRetry1.setChecked(true);
        } else if (retryCount == 3) {
            binding.radioRetry3.setChecked(true);
        } else if (retryCount == 5) {
            binding.radioRetry5.setChecked(true);
        } else if (retryCount == 10) {
            binding.radioRetry10.setChecked(true);
        }
        binding.radioLyricRetry.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int value = 3;
                if (checkedId == binding.radioRetry1.getId()) {
                    value = 1;
                } else if (checkedId == binding.radioRetry3.getId()) {
                    value = 3;
                } else if (checkedId == binding.radioRetry5.getId()) {
                    value = 5;
                } else if (checkedId == binding.radioRetry10.getId()) {
                    value = 10;
                }
                PreferencesManager.getInstance(SettingsActivity.this).putInt("lyric_retry_count", value);
            }
        });

        // 蜂窝网络自动降质开关设置
        boolean cellularAutoLower = PreferencesManager.getInstance(this).getBoolean("cellular_auto_lower", true);
        binding.switchCellularAutoLower.setChecked(cellularAutoLower);
        binding.switchCellularAutoLower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferencesManager.getInstance(SettingsActivity.this).putBoolean("cellular_auto_lower", isChecked);
        });

        // 蜂窝网络音质设置
        String cellularQuality = PreferencesManager.getInstance(this).getString("cellular_play_quality", "standard");
        if (cellularQuality.equals("lossless")) {
            binding.radioCellularLossless.setChecked(true);
        } else if (cellularQuality.equals("high")) {
            binding.radioCellularHigh.setChecked(true);
        } else if (cellularQuality.equals("standard")) {
            binding.radioCellularStandard.setChecked(true);
        } else if (cellularQuality.equals("fluent")) {
            binding.radioCellularFluent.setChecked(true);
        }
        binding.radioCellularQuality.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String value = "standard";
                if (checkedId == binding.radioCellularLossless.getId()) {
                    value = "lossless";
                } else if (checkedId == binding.radioCellularHigh.getId()) {
                    value = "high";
                } else if (checkedId == binding.radioCellularStandard.getId()) {
                    value = "standard";
                } else if (checkedId == binding.radioCellularFluent.getId()) {
                    value = "fluent";
                }
                PreferencesManager.getInstance(SettingsActivity.this).putString("cellular_play_quality", value);
            }
        });
    }

    private String getDefaultDownloadPath() {
        return "/storage/emulated/0/音乐搜索/音频/";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 持久化权限
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //noinspection WrongConstant
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                // 将 content URI 转为真实文件路径（Aria 不支持 content://）
                String path = uriToFilePath(uri);
                if (path == null) {
                    path = getDefaultDownloadPath();
                }
                if (!path.endsWith("/")) path += "/";
                downloadPath = path;
                PreferencesManager.getInstance(this).putString("download_path", downloadPath);
                TextView textDownloadPath = findViewById(R.id.text_download_path);
                textDownloadPath.setText(downloadPath);
            }
        }
    }

    /**
     * 将 SAF content URI 转为真实文件路径
     * 例如 content://com.android.externalstorage.documents/tree/primary%3A音乐搜索%2F音频
     * 转为 /storage/emulated/0/音乐搜索/音频
     */
    private String uriToFilePath(Uri uri) {
        String docId = android.provider.DocumentsContract.getTreeDocumentId(uri);
        if (docId.startsWith("primary:")) {
            String relativePath = docId.substring("primary:".length());
            // URL 解码
            try {
                relativePath = java.net.URLDecoder.decode(relativePath, "UTF-8");
            } catch (Exception e) {
                return null;
            }
            return android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + relativePath;
        }
        return null;
    }
} 