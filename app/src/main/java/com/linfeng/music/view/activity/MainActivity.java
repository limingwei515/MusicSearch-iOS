package com.linfeng.music.view.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.R;
import com.linfeng.music.databinding.ActivityMainBinding;
import com.linfeng.music.utils.ConfigsRepository;
import com.linfeng.music.utils.LocalConfigs;
import com.linfeng.music.view.base.BaseActivity;
import com.linfeng.music.viewmodel.MainViewModel;

import java.util.Map;
import java.util.Objects;

public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private MainViewModel viewModel;
    private MotionLayout motionLayout;
    private CountDownTimer countDownTimer;
    private Map<String, String> configs;

    @Override
    protected ActivityMainBinding initViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {
        ImmersionBar.with(this)
                .statusBarView(binding.barView)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.android_background_color)
                .navigationBarDarkIcon(!isNightMode())
                .fitsSystemWindows(true)
                .init();
    }

    @Override
    protected void initActivity() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        motionLayout = binding.getRoot();
        initConfig();
        initMotionListener();
    }

    private void initConfig() {
        ConfigsRepository.setConfigs(LocalConfigs.getLocalConfigs());
        configs = (Map<String, String>) ConfigsRepository.getConfigs().get("启动页控制");

        RequestListener<Drawable> listener = new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                if (motionLayout.getCurrentState() == motionLayout.getStartState()) {
                    motionLayout.transitionToEnd();
                }
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                if (motionLayout.getCurrentState() == motionLayout.getStartState()) {
                    motionLayout.transitionToEnd();
                }
                return false;
            }
        };

        Glide.with(this)
                .load(configs.get("启动页背景图"))
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(listener)
                .into(binding.image);
    }

    private void initMotionListener() {
        motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {

            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                String subTitle = configs.get("启动页副标题");
                TransitionSet transitionSet = new TransitionSet()
                        .addTransition(new ChangeBounds())
                        .addTransition(new Fade(Fade.IN));

                TransitionManager.beginDelayedTransition(
                        (ViewGroup) binding.subtitle.getParent(),
                        transitionSet
                );
                binding.subtitle.setText(subTitle);
                binding.progressCircular.setIndeterminate(false);
                binding.progressCircular.setMax(Integer.parseInt(Objects.requireNonNull(configs.get("启动页倒计时"))));
                startCountTimer();
                binding.skip.setOnClickListener(v -> {
                    countDownTimer.cancel();
                    Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                    startActivity(intent);
                    finish();
                });
                binding.image.setOnClickListener(v -> {
                    if (!Objects.equals(configs.get("启动页跳转地址"), "")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(configs.get("启动页跳转地址")));
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {

            }
        });
    }

    private void startCountTimer() {
        countDownTimer = new CountDownTimer((Integer.parseInt(Objects.requireNonNull(configs.get("启动页倒计时"))) + 1) * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.skipText.setText(String.valueOf(millisUntilFinished / 1000));
                binding.progressCircular.setProgress((int) (millisUntilFinished / 1000), true);
            }

            @Override
            public void onFinish() {
                Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(intent);
                finish();
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
