package com.linfeng.music.view.activity;

import com.linfeng.music.databinding.ActivityAboutBinding;
import com.linfeng.music.view.base.BaseActivity;

public class AboutActivity extends BaseActivity<ActivityAboutBinding> {


    @Override
    protected ActivityAboutBinding initViewBinding() {
        return ActivityAboutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {

    }

    @Override
    protected void initActivity() {
    }
}