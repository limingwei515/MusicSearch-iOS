package com.linfeng.music.view.activity;

import androidx.recyclerview.widget.GridLayoutManager;

import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTaskListener;
import com.arialyy.aria.core.task.DownloadTask;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.R;
import com.linfeng.music.databinding.ActivityDownloadBinding;
import com.linfeng.music.view.adapter.DownloadAdapter;
import com.linfeng.music.view.base.BaseActivity;

import java.util.Collections;
import java.util.List;

public class DownloadActivity extends BaseActivity<ActivityDownloadBinding> implements DownloadTaskListener {
    private DownloadAdapter adapter;
    private List<DownloadEntity> cachedTaskList; // 缓存任务列表

    @Override
    protected ActivityDownloadBinding initViewBinding() {
        return ActivityDownloadBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {
        ImmersionBar.with(this)
                .titleBar(binding.appbar)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.android_background_color)
                .navigationBarDarkIcon(!isNightMode())
                .fitsSystemWindows(true) // 保持系统栏占位
                .init();
    }

    @Override
    protected void initActivity() {
        // 初始化 RecyclerView
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
        binding.rec.setLayoutManager(gridLayoutManager);
        binding.rec.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        binding.rec.setHasFixedSize(true);
        
        // 先显示缓存数据，避免空白等待
        if (cachedTaskList != null && !cachedTaskList.isEmpty()) {
            adapter = new DownloadAdapter(DownloadActivity.this, cachedTaskList);
            binding.rec.setAdapter(adapter);
        }
        
        // 设置工具栏
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(DownloadActivity.this);
            builder.setTitle("温馨提示")
                    .setMessage("确认删除全部下载记录（同步删除文件源）？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        new Thread(() -> {
                            Aria.download(this).removeAllTask(true);
                            runOnUiThread(() -> {
                                if (adapter != null) {
                                    adapter.clearFile();
                                }
                                // 清空缓存
                                if (cachedTaskList != null) {
                                    cachedTaskList.clear();
                                }
                                useToast("已清空下载记录！");
                            });
                        }).start();
                    }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次进入页面时重新加载数据
        loadDownloadTasks();
    }
    
    private void loadDownloadTasks() {
        // 渲染列表数据
        Aria.download(this).register();
        // 异步加载任务列表
        new Thread(() -> {
            List<DownloadEntity> totalTaskList = Aria.download(this).getTaskList();
            // 缓存任务列表
            cachedTaskList = totalTaskList;
            runOnUiThread(() -> {
                if (totalTaskList != null && !totalTaskList.isEmpty()) {
                    Collections.reverse(totalTaskList);// 反转下载列表
                    // 如果适配器已存在，更新数据
                    if (adapter != null) {
                        adapter = new DownloadAdapter(DownloadActivity.this, totalTaskList);
                        binding.rec.setAdapter(adapter);
                    } else {
                        // 否则创建新适配器
                        adapter = new DownloadAdapter(DownloadActivity.this, totalTaskList);
                        binding.rec.setAdapter(adapter);
                    }
                } else {
                    // 清空适配器，避免显示旧数据
                    if (adapter != null) {
                        adapter.clearFile();
                    }
                }
            });
        }).start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时注销 Aria 监听器，避免内存泄漏
        Aria.download(this).unRegister();
    }

    @Override
    public void onWait(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onPre(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskPre(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskResume(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskStart(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskStop(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskCancel(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskFail(DownloadTask task, Exception e) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskComplete(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onTaskRunning(DownloadTask task) {
        adapter.updateState(task);
    }

    @Override
    public void onNoSupportBreakPoint(DownloadTask task) {
        adapter.updateState(task);
    }
}