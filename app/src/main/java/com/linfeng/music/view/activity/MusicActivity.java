package com.linfeng.music.view.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.arialyy.aria.core.Aria;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.linfeng.music.R;
import com.linfeng.music.bean.PlayingMode;
import com.linfeng.music.database.MyLikeMusicDB;
import com.linfeng.music.databinding.ActivityMusicBinding;
import com.linfeng.music.service.MusicLibraryService;
import com.linfeng.music.utils.CommonUtils;
import com.linfeng.music.utils.ConfigsRepository;
import com.linfeng.music.utils.PreferencesManager;
import com.linfeng.music.view.base.BaseActivity;
import com.linfeng.music.view.fragment.HomeFragment;
import com.linfeng.music.view.fragment.StorageDialogFragment;
import com.linfeng.music.view.fragment.UpdateDialogFragment;
import com.linfeng.music.viewmodel.HomeViewModel;
import com.linfeng.music.viewmodel.MusicPlayerViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jp.wasabeef.glide.transformations.BlurTransformation;
import android.content.res.Configuration;

public class MusicActivity extends BaseActivity<ActivityMusicBinding> {

    private MusicPlayerViewModel viewModel;
    private MusicLibraryService musicPlayService;
    private PlayingMode currentMode = PlayingMode.SHUNXU;

    private String nowDownloadPath, nowDownloadTonalName;

    private BottomSheetBehavior<MaterialCardView> behavior;

    // 增加成员变量用于防止重复点击下载按钮
    private boolean isDownloadLoading = false;

    @Override
    protected ActivityMusicBinding initViewBinding() {
        return ActivityMusicBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initStatusBar() {
        ImmersionBar.with(this)
                .statusBarColor(R.color.android_background_color)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.surface_variant_color)
                .init();
    }

    @Override
    protected void initActivity() {
        Aria.download(this).register();
        viewModel = new ViewModelProvider(this).get(MusicPlayerViewModel.class);
        initDialog();
        initFragmentContainer();
        initBottomSheet();
        initMyLikeMusicDB();
        initMusicPlayerService();
        initMusicPlayerListener();
    }

    //======== Android程序数据库 ========//
    private void initMyLikeMusicDB() {
        // 创建或打开数据库（会自动创建表）
        try (MyLikeMusicDB dbHelper = new MyLikeMusicDB(this);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            // 可以在这里进行数据库操作
        }
    }


    //======== 各种dialog相关内容 ========//
    private void initDialog() {
        // 1.检查是否拥有外部存储权限
        String[] permissions = {Permission.MANAGE_EXTERNAL_STORAGE, Permission.POST_NOTIFICATIONS};
        boolean granted = XXPermissions.isGranted(getApplication(), permissions);
        if (!granted) {
            // 没有存储权限 --- 打开获取存储权限弹窗
            new StorageDialogFragment().show(getSupportFragmentManager(), "StorageDialog");
        } else {
            // 拥有存储权限 --- 检查并创建存放音乐的文件夹
            CommonUtils.createDirs(this, "音频");
        }
        // 2.检查当前软件是否需要更新（已关闭）
        // Map<String, String> configs = (Map<String, String>) ConfigsRepository.getConfigs().get("软件更新控制");
        // if (!Objects.equals(configs.get("当前版本"), CommonUtils.getVersionName(getApplication(), "1.0.0"))) {
        //     new UpdateDialogFragment().show(getSupportFragmentManager(), "UpdateDialogFragment");
        // }
    }

    private void initFragmentContainer() {
        showLoadingDialog();
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.initBangConfig();
        homeViewModel.initSongConfig();
        // 这里当数据渲染成功后打开Fragment
        homeViewModel.getBangList().observe(this, datas -> {
            homeViewModel.getSongList().observe(this, datas1 -> {
                if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                            )
                            .add(R.id.fragment_container, HomeFragment.newInstance(), "HomeFragment")
                            .commit();
                    loadingDialog.dismiss();
                }
            });
        });
    }

    //======== BottomSheet相关事件 ========//
    private void initBottomSheet() {
        // bottomSheet监听事件 --- 底部音乐详情滑动栏
        behavior = BottomSheetBehavior.from(binding.bottomSheet);
        binding.sheetLayout.setOnClickListener(v -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 根据 slideOffset 调整透明度
                float alphaSheetLayout = 1.0f - slideOffset; // 从 1.0 渐变到 0.0
                float cardRadius = dp2px(20) - slideOffset * dp2px(20);
                binding.sheetLayout.setAlpha(alphaSheetLayout);
                binding.sheetContent.setAlpha(slideOffset);
                binding.bottomSheet.setRadius(cardRadius);
            }
        });

        // 这里设置用户点击返回的事件监听 --- 优先关闭 loadingDialog，其次收起 BottomSheet，最后退出界面
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    closeBottomSheet();
                } else {
                    finish(); // 直接退出界面
                }
            }
        });
    }

    public void closeBottomSheet() {
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public boolean isBottomSheetExpanded() {
        BottomSheetBehavior<MaterialCardView> behavior = BottomSheetBehavior.from(binding.bottomSheet);
        return behavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    //======== PlayerService相关事件 ========//
    private void initMusicPlayerService() {
        // 1.链接Service服务
        Intent intent = new Intent(this, MusicLibraryService.class);
        startService(intent);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                // 将Service服务实例传递给ViewModel --- 用于音乐的控制
                MusicLibraryService.MusicBinder binder = (MusicLibraryService.MusicBinder) iBinder;
                musicPlayService = binder.getService();
                viewModel.initMusicPlayService(musicPlayService);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    //======== BottomSheet-UI设置 ========//
    private void initMusicPlayerListener() {
        // 设置按钮选择点击事件
        setupToggleSwitch();

        // 设置当前正在播放的音乐的UI数据
        viewModel.getNowPlayingMusicData().observe(this, musicBean -> {
            binding.songName.setText(musicBean.getName());
            binding.sheetName.setText(musicBean.getName());
            binding.singerName.setText(musicBean.getArtist());
            binding.sheetSubname.setText(musicBean.getArtist());

            // 渲染图片列表
            Glide.with(MusicActivity.this)
                    .load(musicBean.getAlbumIcon())
                    .placeholder(R.mipmap.ic_launcher)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.sheetIcon);

            Glide.with(MusicActivity.this)
                    .load(musicBean.getAlbumIcon())
                    .placeholder(R.mipmap.ic_launcher)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.image);

            // 使用 Glide 加载并应用模糊效果
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new BlurTransformation(8, 2)) // 调小模糊半径和采样率，提升清晰度
                    .diskCacheStrategy(DiskCacheStrategy.ALL); // 缓存策略
            // 渲染图片列表
            Glide.with(MusicActivity.this)
                    .load(musicBean.getAlbumIcon())
                    .apply(requestOptions)
                    .into(binding.imageBack);

            // 设置下载按钮点击事件
            binding.sheetListButton.setOnClickListener(view -> {
                if (isDownloadLoading) return;
                isDownloadLoading = true;
                showLoadingDialog();
                viewModel.getMusicDetail();
            });
            binding.switchButton.setOnClickListener(view -> {
                if (isDownloadLoading) return;
                isDownloadLoading = true;
                showLoadingDialog();
                viewModel.getMusicDetail();
            });
        });

        // 设置当前正在播放的音乐的进度
        binding.progressCircular.animate();
        viewModel.getNowMusicPlayingProgress().observe(this, musicProgress -> {
            // 根据当前秒数更新歌词
            binding.sheetViewpager.updateTime(musicProgress, false);
            binding.progressCircular.setProgress(musicProgress, true);
            binding.seekbar.setValue(musicProgress);
            binding.time.setText(CommonUtils.formatTime(musicProgress));
        });
        viewModel.getNowMusicPlayingDuration().observe(this, musicDuration -> {
            binding.progressCircular.setMax(musicDuration);
            binding.seekbar.setValueTo(musicDuration);
            binding.seekbar.setLabelFormatter(value -> CommonUtils.formatTime((int) value));
            binding.max.setText(CommonUtils.formatTime(musicDuration));
        });

        // 设置播放按钮的点击事件
        binding.sheetPlayButton.setOnClickListener(v -> viewModel.toggleMusic());
        binding.playFab.setOnClickListener(v -> viewModel.toggleMusic());
        viewModel.getIsPlaying().observe(this, isPlaying -> {
            if (!isPlaying) {
                binding.sheetPlayButton.setIconResource(R.drawable.icon_play_arrow);
                binding.playFab.setImageResource(R.drawable.icon_play_arrow);
            } else {
                binding.sheetPlayButton.setIconResource(R.drawable.icon_pause);
                binding.playFab.setImageResource(R.drawable.icon_pause);
            }
        });

        // 设置切换上下音乐
        binding.nextButton.setOnClickListener(v -> viewModel.playNextMusic());
        binding.prevButton.setOnClickListener(v -> viewModel.playPreMusic());


        // 设置歌曲播放模式切换
        binding.playStatus.setOnClickListener(v -> {
            currentMode = currentMode.next();
            binding.playStatus.setIconResource(currentMode.getIconRes());
            viewModel.togglePlayStatus(currentMode);
        });


        // 拖动进度条实现更改播放位置
        binding.seekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                viewModel.seekTo((int) slider.getValue());
            }
        });

        // 设置歌词
        binding.sheetViewpager.setCurrentColor(getColor(R.color.main_primary));
        binding.sheetViewpager.setCurrentTextSize(dp2px(20));
        binding.sheetViewpager.setNormalColor(getColor(R.color.main_title_color));
        binding.sheetViewpager.setNormalTextSize(dp2px(14));
        binding.sheetViewpager.setSentenceDividerHeight(dp2px(12));
        binding.sheetViewpager.setLabel("暂无歌词");
        viewModel.getLyricList().observe(this, lyricList -> {
            if (lyricList != null) {
                // 过滤掉内容为空的歌词（去除歌词和翻译之间的空行）
                List<com.dirror.lyricviewx.LyricEntry> filtered = new ArrayList<>();
                for (com.dirror.lyricviewx.LyricEntry entry : lyricList) {
                    if (entry != null && entry.getText() != null && !entry.getText().trim().isEmpty()) {
                        filtered.add(entry);
                    }
                }
                binding.sheetViewpager.setLyricEntryList(filtered);
            }
        });

        // 处理音乐下载音质的选项弹窗
        viewModel.getMusicTonalList().observe(this, list -> {
            loadingDialog.dismiss();
            isDownloadLoading = false; // 关闭loading时允许再次点击
            if (list == null || list.isEmpty()) {
                Snackbar.make(binding.fragmentContainer, "获取音质信息失败", Snackbar.LENGTH_SHORT).show();
                return;
            }
            // 1.遍历集合将key赋值
            List<String> tonalName = new ArrayList<>();
            List<String> tonalBitrate = new ArrayList<>();
            List<String> tonalPath = new ArrayList<>();
            for (Map<String, String> map : list) {
                if (map.containsKey("2000") && map.get("2000") != null && !map.get("2000").trim().isEmpty()) {
                    tonalName.add("无损" + "(" + map.get("2000") + ")");
                    tonalBitrate.add("2000");
                    tonalPath.add("flac");
                }
                if (map.containsKey("320") && map.get("320") != null && !map.get("320").trim().isEmpty()) {
                    tonalName.add("高品" + "(" + map.get("320") + ")");
                    tonalBitrate.add("320kmp3");
                    tonalPath.add("mp3");
                }
                if (map.containsKey("128") && map.get("128") != null && !map.get("128").trim().isEmpty()) {
                    tonalName.add("标准" + "(" + map.get("128") + ")");
                    tonalBitrate.add("128");
                    tonalPath.add("mp3");
                }
                if (map.containsKey("48") && map.get("48") != null && !map.get("48").trim().isEmpty()) {
                    tonalName.add("流畅" + "(" + map.get("48") + ")");
                    tonalBitrate.add("48");
                    tonalPath.add("aac");
                }
            }
            int[] chooseItems = {0};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MusicActivity.this);
            builder.setTitle("音质选择")
                    .setPositiveButton("下载", (dialog, which) -> {
                        // 从设置读取下载目录
                        String userPath = PreferencesManager.getInstance(MusicActivity.this).getString("download_path", "");
                        if (userPath == null || userPath.isEmpty()) {
                            userPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) + "/音频/";
                        }
                        if (!userPath.endsWith("/")) userPath += "/";
                        nowDownloadPath = userPath + viewModel.getNowPlayingMusicDataValue().getArtist() + " - " + viewModel.getNowPlayingMusicDataValue().getName() + "." + tonalPath.get(chooseItems[0]);
                        nowDownloadTonalName = tonalName.get(chooseItems[0]).substring(0, 2);
                        viewModel.getMusicDownloadUrl(tonalBitrate.get(chooseItems[0]));
                    })
                    .setNegativeButton("取消", null)
                    .setSingleChoiceItems(tonalName.toArray(new String[0]), 0,
                            (dialog, which) -> {
                                chooseItems[0] = which;
                            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        viewModel.getMusicDownloadUrl().observe(this, downloadUrl -> {
            Aria.download(this)
                    .load(downloadUrl)
                    .ignoreCheckPermissions()
                    .ignoreFilePathOccupy()
                    .setExtendField((viewModel.getNowPlayingMusicDataValue().getAlbumIcon()) + "|" + nowDownloadTonalName)
                    .setFilePath(nowDownloadPath)
                    .create();

            // 开始下载后弹出snackBar提示
            Snackbar snackbar = Snackbar.make(binding.fragmentContainer, "已添加到下载列表", Snackbar.LENGTH_SHORT).setAction("前往", v1 -> {
                Intent intent = new Intent(MusicActivity.this, DownloadActivity.class);
                startActivity(intent);
            });
            View view = snackbar.getView();
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setCornerRadius(dp2px(20));
            view.setBackground(gradientDrawable);
            snackbar.show();
        });
    }

    private void setupToggleSwitch() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle);
        View lyricView = findViewById(R.id.sheet_viewpager);
        View imageCard = findViewById(R.id.image_card);
        View imageBack = findViewById(R.id.image_back);

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            // 切换前先清除动画，避免动画冲突导致空白
            imageCard.clearAnimation();
            imageBack.clearAnimation();
            lyricView.clearAnimation();
            if (checkedId == R.id.b1) {
                // 显示图片，隐藏歌词
                fadeInView(imageCard);
                fadeInView(imageBack);
                fadeOutView(lyricView);
            } else if (checkedId == R.id.b2) {
                // 显示歌词，隐藏图片
                fadeOutView(imageCard);
                fadeOutView(imageBack);
                fadeInView(lyricView);
            }
        });
    }

    private void fadeInView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .setListener(null);
        }
    }

    private void fadeOutView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 切换深色/浅色模式时，强制关闭loadingDialog，防止卡住
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 删除返回键处理相关代码，避免重复注册
    }
}
