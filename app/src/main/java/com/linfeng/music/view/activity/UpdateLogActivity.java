package com.linfeng.music.view.activity;

import androidx.recyclerview.widget.GridLayoutManager;

import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.R;
import com.linfeng.music.databinding.ActivityUpdateLogBinding;
import com.linfeng.music.view.adapter.UpdateLogAdapter;
import com.linfeng.music.view.base.BaseActivity;

import org.json.JSONException;

public class UpdateLogActivity extends BaseActivity<ActivityUpdateLogBinding> {


    @Override
    protected ActivityUpdateLogBinding initViewBinding() {
        return ActivityUpdateLogBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {
        // 主界面沉浸式配置
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
        try {
            UpdateLogAdapter updateLogAdapter = new UpdateLogAdapter(this);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
            binding.rec.setAdapter(updateLogAdapter);
            binding.rec.setLayoutManager(gridLayoutManager);
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}