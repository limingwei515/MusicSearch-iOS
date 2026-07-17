package com.linfeng.music.view.fragment;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.arialyy.aria.core.Aria;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.R;
import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.databinding.DialogMusicSearchBinding;
import com.linfeng.music.databinding.FragmentMusicBinding;
import com.linfeng.music.view.activity.DownloadActivity;
import com.linfeng.music.view.activity.MusicActivity;
import com.linfeng.music.view.adapter.MusicAdapter;
import com.linfeng.music.view.base.BaseFragment;
import com.linfeng.music.viewmodel.MusicPlayerViewModel;
import com.linfeng.music.viewmodel.MusicViewModel;
import com.linfeng.music.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.content.res.Configuration;
import androidx.annotation.NonNull;

public class MusicFragment extends BaseFragment<FragmentMusicBinding> {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";

    /**
     * search --- 搜索歌曲的页面类型
     * <p>
     * like --- 收藏音乐的页面类型
     * <p>
     * bang --- 榜单歌曲的页面类型
     * <p>
     * playlist --- 歌单歌曲的页面类型
     */
    private String musicType;
    private String musicId;
    private String toolTitle;

    private MusicViewModel musicViewModel;
    private MusicPlayerViewModel musicPlayerViewModel;

    private int page = 0;

    private MusicAdapter musicAdapter;

    private MusicBean nowDownloadMusic;
    private String nowDownloadPath;
    private String nowDownloadTonalName;

    private long lastDownloadClickTime = 0;
    private OnBackPressedCallback backPressedCallback;

    public MusicFragment() {

    }

    public static MusicFragment newInstance(String musicType, String musicId, String title) {
        MusicFragment fragment = new MusicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, musicType);
        args.putString(ARG_PARAM2, musicId);
        args.putString(ARG_PARAM3, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            musicType = getArguments().getString(ARG_PARAM1);
            musicId = getArguments().getString(ARG_PARAM2);
            toolTitle = getArguments().getString(ARG_PARAM3);
        }
    }

    @Override
    protected FragmentMusicBinding initViewBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMusicBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initStatusBar() {
        // 主界面沉浸式配置
        ImmersionBar.with(this)
                .titleBar(binding.appbar)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.surface_variant_color)
                .navigationBarDarkIcon(!isNightMode())
                .fitsSystemWindows(true) // 保持系统栏占位
                .init();
    }

    @Override
    protected void initFragment() {
        Aria.download(this).register();
        musicViewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        musicPlayerViewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
        
        initToolBar();
        initMusicList();
        initMusicDownload();
        handleBackPress();
    }


    private void initToolBar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            performBackAction();
        });
        if (toolTitle != null) {
            binding.toolbar.setTitle(toolTitle);
        }
        if (musicType.equals("search")) {
            // 渲染menu
            binding.toolbar.inflateMenu(R.menu.music_menu);
            // 右上角搜索点击事件
            binding.toolbar.setOnMenuItemClickListener(item -> {
                View inflate = getLayoutInflater().inflate(R.layout.dialog_music_search, null);
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
                bottomSheetDialog.setContentView(inflate);
                bottomSheetDialog.show();

                DialogMusicSearchBinding dialogBinding = DialogMusicSearchBinding.bind(Objects.requireNonNull(bottomSheetDialog.findViewById(R.id.dialog_music_search)));
                // 设置布局点击事件等
                dialogBinding.positive.setOnClickListener(v -> {
                    String musicName = Objects.requireNonNull(dialogBinding.searchContent.getText()).toString().trim();
                    if (musicName.isEmpty()) {
                        useToast("请输入要搜索的内容！");
                        return;
                    }
                    // 初始化页面状态
                    musicId = musicName;
                    binding.toolbar.setTitle(musicName);
                    page = 0;
                    binding.srl.autoRefresh();
                    bottomSheetDialog.dismiss();
                });
                dialogBinding.negative.setOnClickListener(v -> bottomSheetDialog.dismiss());
                return true;
            });
        }
    }

    private void initMusicList() {
        // 1. 先设置下拉刷新监听器
        binding.srl.setOnRefreshListener(refreshLayout -> {
            page = 0;
            binding.loadingFail.setVisibility(View.GONE);
            if (musicType.equals("bang")) {
                musicViewModel.initBangMusicData(musicId, page, "100");
            } else if (musicType.equals("playlist")) {
                musicViewModel.initPlayListMusicData(musicId, page, "100");
            } else if (musicType.equals("search")) {
                musicViewModel.initSearchMusicData(musicId, page, "100");
            } else if (musicType.equals("like")) {
                musicViewModel.initLikeMusicData();
            }
        });
        // 设置上拉加载监听器
        binding.srl.setOnLoadMoreListener(refreshLayout -> {
            if (musicType.equals("bang")) {
                musicViewModel.initBangMusicData(musicId, ++page, "100");
            } else if (musicType.equals("playlist")) {
                musicViewModel.initPlayListMusicData(musicId, ++page, "100");
            } else if (musicType.equals("search")) {
                musicViewModel.initSearchMusicData(musicId, ++page, "100");
            }
        });

        // 2. 再设置数据观察者（LiveData 是 Activity 级别的，需先注册观察再触发请求）
        // 榜单歌曲数据观察者
        musicViewModel.getBangMusicData().observe(getViewLifecycleOwner(), data -> {
            if (musicType.equals("bang")) {
                updateMusicList(data, page);
            }
        });
        // 歌单歌曲数据观察者
        musicViewModel.getPlayListMusicData().observe(getViewLifecycleOwner(), data -> {
            if (musicType.equals("playlist")) {
                updateMusicList(data, page);
            }
        });
        // 搜索歌曲数据观察者
        musicViewModel.getSearchMusicData().observe(getViewLifecycleOwner(), data -> {
            if (musicType.equals("search")) {
                updateMusicList(data, page);
            }
        });
        // 收藏歌曲数据观察者
        musicViewModel.getLikeMusicData().observe(getViewLifecycleOwner(), data -> {
            if (musicType.equals("like")) {
                if (data.isEmpty()) {
                    binding.loadingFail.setVisibility(View.VISIBLE);
                    binding.tip.setText("当前没有收藏的歌曲");
                    binding.srl.finishRefresh();
                    binding.srl.setEnableLoadMore(false);
                    return;
                }
                musicAdapter = new MusicAdapter(data, requireContext(), true);
                binding.musicRec.setAdapter(musicAdapter);
                binding.musicRec.setLayoutManager(new GridLayoutManager(requireContext(), 1));
                initMusicOnItemClick();
                binding.srl.finishRefresh();
                binding.srl.setEnableLoadMore(false);
            }
        });

        // 没有更多数据提示
        musicViewModel.getHasLoadingMoreData().observe(getViewLifecycleOwner(), isLoadingMore -> {
            if (!isLoadingMore) {
                binding.srl.finishLoadMoreWithNoMoreData();
            }
        });

        // 加载失败提示
        musicViewModel.getIsLoadingSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            binding.srl.finishRefresh(false);
            binding.loadingFail.setVisibility(View.VISIBLE);
        });

        // 3. 最后手动触发数据请求（新 Fragment 打开时立即刷新列表）
        page = 0;
        binding.loadingFail.setVisibility(View.GONE);
        if (musicType.equals("bang")) {
            musicViewModel.initBangMusicData(musicId, page, "100");
        } else if (musicType.equals("playlist")) {
            musicViewModel.initPlayListMusicData(musicId, page, "100");
        } else if (musicType.equals("search")) {
            musicViewModel.initSearchMusicData(musicId, page, "100");
        } else if (musicType.equals("like")) {
            musicViewModel.initLikeMusicData();
        }
    }

    private void updateMusicList(List<MusicBean> data, int currentPage) {
        if (currentPage == 0) {
            musicAdapter = new MusicAdapter(data, requireContext(), true);
            binding.musicRec.setAdapter(musicAdapter);
            binding.musicRec.setLayoutManager(new GridLayoutManager(requireContext(), 1));
            initMusicOnItemClick();
            binding.srl.finishRefresh();
        } else {
            if (musicAdapter != null) {
                musicAdapter.setData(data);
                musicAdapter.notifyItemRangeChanged((currentPage + 1) * 30, 30);
            }
            binding.srl.finishLoadMore();
        }
    }

    private void initMusicOnItemClick() {
        if (musicAdapter != null) {
            // 点击整个列表项 → 播放音乐
            musicAdapter.setOnItemClickListener((holder, musicBean, position) -> {
                musicPlayerViewModel.playMusic(musicBean.getId(), position, musicAdapter.getAllData());
            });
            // 点击下载按钮 → 弹出下载选择
            musicAdapter.setOnDownloadClickListener(musicBean -> {
                long now = System.currentTimeMillis();
                if (now - lastDownloadClickTime < 800) return;
                lastDownloadClickTime = now;
                showLoadingDialog();
                musicViewModel.getMusicDetail(musicBean.getId());
                nowDownloadMusic = musicBean;
            });
        }
    }

    private void initMusicDownload() {
        musicViewModel.getMusicTonalList().observe(this, list -> {
            if (loadingDialog != null) loadingDialog.dismiss();
            if (list == null || list.isEmpty()) return;
            // 1.遍历集合将key赋值
            List<String> tonalName = new ArrayList<>();
            List<String> tonalBitrate = new ArrayList<>();
            List<String> tonalPath = new ArrayList<>();
            for (Map<String, String> map : list) {
                if (map.containsKey("2000")) {
                    tonalName.add("无损" + "(" + map.get("2000") + ")");
                    tonalBitrate.add("2000");
                    tonalPath.add("flac");
                }
                if (map.containsKey("320")) {
                    tonalName.add("高品" + "(" + map.get("320") + ")");
                    tonalBitrate.add("320kmp3");
                    tonalPath.add("mp3");
                }
                if (map.containsKey("128")) {
                    tonalName.add("标准" + "(" + map.get("128") + ")");
                    tonalBitrate.add("128");
                    tonalPath.add("mp3");
                }
                if (map.containsKey("48")) {
                    tonalName.add("流畅" + "(" + map.get("48") + ")");
                    tonalBitrate.add("48");
                    tonalPath.add("aac");
                }
            }
            int[] chooseItems = {0};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle("音质选择")
                    .setPositiveButton("下载", (dialog, which) -> {
                        // 读取用户自定义下载路径
                        if (nowDownloadMusic == null) return;
                        String userPath = PreferencesManager.getInstance(requireContext()).getString("download_path", "");
                        String fileName = nowDownloadMusic.getArtist() + " - " + nowDownloadMusic.getName() + "." + tonalPath.get(chooseItems[0]);
                        if (userPath != null && !userPath.isEmpty()) {
                            // 确保是文件路径而非 content URI
                            if (userPath.startsWith("content://")) {
                                userPath = "/storage/emulated/0/音乐搜索/音频/";
                            }
                            if (!userPath.endsWith("/")) userPath += "/";
                            nowDownloadPath = userPath + fileName;
                        } else {
                            nowDownloadPath = "/storage/emulated/0/音乐搜索/音频/" + fileName;
                        }
                        nowDownloadTonalName = tonalName.get(chooseItems[0]).substring(0, 2);
                        musicViewModel.getMusicDownloadUrl(nowDownloadMusic.getId(), tonalBitrate.get(chooseItems[0]));
                    })
                    .setNegativeButton("取消", null)
                    .setSingleChoiceItems(tonalName.toArray(new String[0]), 0,
                            (dialog, which) -> {
                                chooseItems[0] = which;
                            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        musicViewModel.getMusicDownloadUrl().observe(this, downloadUrl -> {
            if (nowDownloadMusic == null || nowDownloadPath == null) return;
            Aria.download(this)
                    .load(downloadUrl)
                    .ignoreCheckPermissions()
                    .ignoreFilePathOccupy()
                    .setExtendField((nowDownloadMusic.getAlbumIcon()) + "|" + nowDownloadTonalName)
                    .setFilePath(nowDownloadPath)
                    .create();

            // 开始下载后弹出snackBar提示
            requireActivity().runOnUiThread(() -> {
                Snackbar snackbar = Snackbar.make(binding.drawer, "已添加到下载列表", Snackbar.LENGTH_SHORT).setAction("前往", v1 -> {
                    Intent intent = new Intent(requireContext(), DownloadActivity.class);
                    startActivity(intent);
                });
                View view = snackbar.getView();
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setCornerRadius(dp2px(20));
                view.setBackground(gradientDrawable);
                snackbar.show();
            });
        });
    }

    private void handleBackPress() {
        if (backPressedCallback != null) {
            backPressedCallback.remove();
        }
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 优先关闭loadingDialog
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                    return;
                }
                // 1. 检查 Activity 中的 BottomSheet 是否展开
                if (((MusicActivity) requireActivity()).isBottomSheetExpanded()) {
                    // BottomSheet 展开时，不消费事件，交由 Activity 处理
                    ((MusicActivity) requireActivity()).closeBottomSheet();
                    return;
                }
                // 2. 如果 BottomSheet 已折叠，执行 Fragment 返回逻辑
                performBackAction();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
    }

    private void performBackAction() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            // 如果有回退栈，直接弹出
            getParentFragmentManager().popBackStack();
        } else {
            // 没有回退栈的情况下，使用原来的方式
            HomeFragment homeFragment = (HomeFragment) requireActivity().getSupportFragmentManager()
                    .findFragmentByTag("HomeFragment");
            if (homeFragment != null) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        )
                        .remove(MusicFragment.this)
                        .show(homeFragment)
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 切换深色/浅色模式时，关闭loadingDialog并直接返回主界面，防止卡住
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        performBackAction();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 离开时也关闭loadingDialog，防止卡住
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}