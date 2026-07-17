package com.linfeng.music.view.fragment;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import android.text.Html;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gyf.immersionbar.ImmersionBar;
import com.linfeng.music.R;
import com.linfeng.music.bean.BannerBean;
import com.linfeng.music.bean.SongBean;
import com.linfeng.music.databinding.DialogMusicSearchBinding;
import com.linfeng.music.databinding.FragmentHomeBinding;
import com.linfeng.music.utils.ConfigsRepository;
import com.linfeng.music.view.activity.DownloadActivity;
import com.linfeng.music.view.activity.UpdateLogActivity;
import com.linfeng.music.view.adapter.BangAdapter;
import com.linfeng.music.view.adapter.MyBannerAdapter;
import com.linfeng.music.view.adapter.SongAdapter;
import com.linfeng.music.view.base.BaseFragment;
import com.linfeng.music.viewmodel.HomeViewModel;
import com.youth.banner.indicator.CircleIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private static final String TAG = "HomeFragment-Perf";
    private static final boolean ENABLE_LOGGING = false;
    
    private List<Map<String, String>> configs;
    private HomeViewModel homeViewModel;
    private SongAdapter mySongAdapter;
    private long lastNavClickTime = 0;
    
    private long fragmentStartTime;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private void logPerf(String tag, String message) {
        if (ENABLE_LOGGING) {
            long elapsed = System.currentTimeMillis() - fragmentStartTime;
            Log.d(TAG + "-" + tag, message + " [从Fragment创建: " + elapsed + "ms]");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected FragmentHomeBinding initViewBinding(LayoutInflater inflater, @Nullable ViewGroup container) {
        fragmentStartTime = System.currentTimeMillis();
        logPerf("Lifecycle", "initViewBinding 开始");
        long start = System.currentTimeMillis();
        FragmentHomeBinding binding = FragmentHomeBinding.inflate(inflater, container, false);
        logPerf("Lifecycle", "initViewBinding: " + (System.currentTimeMillis() - start) + "ms");
        return binding;
    }

    @Override
    protected void initStatusBar() {
        logPerf("Lifecycle", "initStatusBar 开始");
        long start = System.currentTimeMillis();
        ImmersionBar.with(this)
                .titleBar(binding.appbar)
                .statusBarDarkFont(!isNightMode())
                .navigationBarColor(R.color.surface_variant_color)
                .navigationBarDarkIcon(!isNightMode())
                .fitsSystemWindows(true)
                .init();

        ImmersionBar.with(this)
                .titleBar(binding.navView)
                .init();
        logPerf("Lifecycle", "initStatusBar: " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    protected void initFragment() {
        logPerf("Lifecycle", "initFragment 开始");
        long start = System.currentTimeMillis();
        
        long t1 = System.currentTimeMillis();
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        logPerf("initFragment", "创建 ViewModel: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        configs = (List<Map<String, String>>) ConfigsRepository.getConfigs().get("首页轮播图");
        logPerf("initFragment", "获取配置: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        binding.ssss.setLayoutTransition(layoutTransition);
        logPerf("initFragment", "设置LayoutTransition: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        initToolbar();
        logPerf("initFragment", "initToolbar: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        initBanner();
        logPerf("initFragment", "initBanner: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        initMySongListObserver();
        logPerf("initFragment", "initMySongListObserver: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        initBangListObserver();
        logPerf("initFragment", "initBangListObserver: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        initHotSongListObserver();
        logPerf("initFragment", "initHotSongListObserver: " + (System.currentTimeMillis() - t1) + "ms");
        
        t1 = System.currentTimeMillis();
        new Thread(() -> {
            long tt = System.currentTimeMillis();
            homeViewModel.initBangConfig();
            logPerf("Network", "initBangConfig: " + (System.currentTimeMillis() - tt) + "ms");
            tt = System.currentTimeMillis();
            homeViewModel.initSongConfig();
            logPerf("Network", "initSongConfig: " + (System.currentTimeMillis() - tt) + "ms");
        }).start();
        logPerf("initFragment", "启动网络请求: " + (System.currentTimeMillis() - t1) + "ms");
        
        logPerf("Lifecycle", "initFragment: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initToolbar() {
        logPerf("Toolbar", "initToolbar 开始");
        long start = System.currentTimeMillis();
        
        binding.toolbar.setNavigationOnClickListener(v -> {
            long t1 = System.currentTimeMillis();
            binding.drawer.open();
            logPerf("Toolbar", "打开Drawer: " + (System.currentTimeMillis() - t1) + "ms");
        });
        
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (getChildFragmentManager().findFragmentByTag("SearchDialog") != null) {
                return true;
            }
            
            long t1 = System.currentTimeMillis();
            View inflate = getLayoutInflater().inflate(R.layout.dialog_music_search, null);
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
            bottomSheetDialog.setContentView(inflate);
            bottomSheetDialog.setOnDismissListener(dialog -> {
                getChildFragmentManager().beginTransaction().remove(getChildFragmentManager().findFragmentByTag("SearchDialog")).commitAllowingStateLoss();
            });
            getChildFragmentManager().beginTransaction().add(new Fragment(), "SearchDialog").commitAllowingStateLoss();
            bottomSheetDialog.show();
            logPerf("Toolbar", "显示搜索对话框: " + (System.currentTimeMillis() - t1) + "ms");

            DialogMusicSearchBinding dialogBinding = DialogMusicSearchBinding.bind(Objects.requireNonNull(bottomSheetDialog.findViewById(R.id.dialog_music_search)));
            dialogBinding.positive.setOnClickListener(v -> {
                String musicName = Objects.requireNonNull(dialogBinding.searchContent.getText()).toString().trim();
                if (musicName.isEmpty()) {
                    useToast("请输入要搜索的内容！");
                    return;
                }
                
                long tt = System.currentTimeMillis();
                HomeFragment homeFragment = (HomeFragment) requireActivity().getSupportFragmentManager()
                        .findFragmentByTag("HomeFragment");
                MusicFragment musicFragment = MusicFragment.newInstance("search", musicName, musicName);
                assert homeFragment != null;
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        )
                        .add(R.id.fragment_container, musicFragment, "MusicFragment")
                        .hide(homeFragment)
                        .addToBackStack(null)
                        .commit();
                bottomSheetDialog.dismiss();
                logPerf("Toolbar", "跳转到搜索页面: " + (System.currentTimeMillis() - tt) + "ms");
            });
            dialogBinding.negative.setOnClickListener(v -> bottomSheetDialog.dismiss());
            return true;
        });
        
        binding.navView.setNavigationItemSelectedListener(item -> {
            long now = System.currentTimeMillis();
            if (now - lastNavClickTime < 800) return false;
            lastNavClickTime = now;
            
            long t1 = System.currentTimeMillis();
            switch (item.getItemId()) {
                case R.id.like:
                    binding.drawer.close();
                    HomeFragment homeFragment = (HomeFragment) requireActivity().getSupportFragmentManager()
                            .findFragmentByTag("HomeFragment");
                    MusicFragment musicFragment = MusicFragment.newInstance("like", null, "我的收藏");
                    assert homeFragment != null;
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            )
                            .add(R.id.fragment_container, musicFragment, "MusicFragment")
                            .hide(homeFragment)
                            .addToBackStack(null)
                            .commit();
                    logPerf("Nav", "跳转到收藏: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
                case R.id.dow:
                    Intent intent = new Intent(requireContext(), DownloadActivity.class);
                    startActivity(intent);
                    logPerf("Nav", "跳转下载页: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
                case R.id.update_log:
                    Intent intent2 = new Intent(requireContext(), UpdateLogActivity.class);
                    startActivity(intent2);
                    logPerf("Nav", "跳转更新日志: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
                case R.id.gedan:
                    binding.drawer.close();
                    DrawerLayout.SimpleDrawerListener drawerListener = new DrawerLayout.SimpleDrawerListener() {
                        @Override
                        public void onDrawerClosed(View drawerView) {
                            super.onDrawerClosed(drawerView);
                            showBangMusicDialog();
                            binding.drawer.removeDrawerListener(this);
                        }
                    };
                    binding.drawer.addDrawerListener(drawerListener);
                    logPerf("Nav", "显示歌单对话框: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
                case R.id.feedback:
                    Map<String, Object> configs1 = ConfigsRepository.getConfigs();
                    Map<String, String> config = (Map<String, String>) configs1.get("杂项配置");
                    String qqGroupUrl = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + config.get("反馈Q群") + "&card_type=group&source=external";
                    Intent joinQQGroupIntent = new Intent();
                    joinQQGroupIntent.setData(Uri.parse(qqGroupUrl));
                    try {
                        startActivity(joinQQGroupIntent);
                    } catch (Exception e) {
                        useToast("未检测到QQ，请安装后重试。");
                    }
                    logPerf("Nav", "打开QQ群: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
                case R.id.setting:
                    Intent intentSetting = new Intent(requireContext(), com.linfeng.music.view.activity.SettingsActivity.class);
                    startActivity(intentSetting);
                    logPerf("Nav", "跳转设置: " + (System.currentTimeMillis() - t1) + "ms");
                    break;
            }
            return false;
        });
        
        logPerf("Toolbar", "initToolbar: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initBanner() {
        logPerf("Banner", "initBanner 开始");
        long start = System.currentTimeMillis();
        
        List<BannerBean> bannerList = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            BannerBean bannerBean = new BannerBean();
            bannerBean.setTitle(configs.get(i).get("标题"));
            bannerBean.setImageUrl(configs.get(i).get("图片地址"));
            bannerBean.setAddressUrl(configs.get(i).get("跳转地址"));
            bannerList.add(bannerBean);
            
            long t1 = System.currentTimeMillis();
            MyBannerAdapter myBannerAdapter = new MyBannerAdapter(bannerList, requireContext());
            binding.banner.setAdapter(myBannerAdapter);
            binding.banner.setIndicator(new CircleIndicator(requireContext()));
            logPerf("Banner", "设置Banner适配器: " + (System.currentTimeMillis() - t1) + "ms");
        }
        
        logPerf("Banner", "initBanner: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initMySongListObserver() {
        logPerf("SongList", "initMySongListObserver 开始");
        long start = System.currentTimeMillis();
        
        binding.musicListRec.setNestedScrollingEnabled(false);
        
        long t1 = System.currentTimeMillis();
        List<SongBean> mySongList = instance.getList(requireContext(), "MySongsList", SongBean.class);
        logPerf("SongList", "读取本地歌曲列表: " + (System.currentTimeMillis() - t1) + "ms");
        
        if (mySongList != null) {
            t1 = System.currentTimeMillis();
            List<SongBean> mySongsList = instance.getList(requireContext(), "MySongsList", SongBean.class);
            mySongAdapter = new SongAdapter(mySongsList, requireContext(), true);
            binding.musicListRec.setAdapter(mySongAdapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
            binding.musicListRec.setLayoutManager(gridLayoutManager);
            logPerf("SongList", "设置歌曲列表: " + (System.currentTimeMillis() - t1) + "ms");
        }
        
        binding.bangMusicBottom.setOnClickListener(view -> showBangMusicDialog());
        
        java.util.concurrent.atomic.AtomicLong callbackStart = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        homeViewModel.getSongDetail().observe(this, datas -> {
            long duration = System.currentTimeMillis() - callbackStart.get();
            logPerf("SongList", "歌单详情加载回调: " + duration + "ms");
            
            loadingDialog.dismiss();
            if (datas.getSongId().equals("error")) {
                useToast("获取歌单数据失败！");
                return;
            }
            
            long tt = System.currentTimeMillis();
            if (mySongList == null) {
                List<SongBean> songBeans = new ArrayList<>();
                songBeans.add(datas);
                instance.putList(requireContext(), "MySongsList", songBeans);
                mySongAdapter = new SongAdapter(songBeans, requireContext(), true);
                binding.musicListRec.setAdapter(mySongAdapter);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
                binding.musicListRec.setLayoutManager(gridLayoutManager);
            } else {
                List<SongBean> mySongsList = instance.getList(requireContext(), "MySongsList", SongBean.class);
                mySongsList.add(datas);
                instance.putList(requireContext(), "MySongsList", mySongsList);
                mySongAdapter.setData(mySongsList);
                mySongAdapter.notifyItemInserted(mySongsList.size() - 1);
            }
            logPerf("SongList", "更新歌曲列表UI: " + (System.currentTimeMillis() - tt) + "ms");
        });
        
        logPerf("SongList", "initMySongListObserver: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void showBangMusicDialog() {
        logPerf("Dialog", "showBangMusicDialog 开始");
        long start = System.currentTimeMillis();
        
        View inflate = getLayoutInflater().inflate(R.layout.dialog_music_search, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(inflate);
        bottomSheetDialog.show();
        
        DialogMusicSearchBinding dialogBinding = DialogMusicSearchBinding.bind(bottomSheetDialog.findViewById(R.id.dialog_music_search));
        dialogBinding.title.setText("绑定歌单");
        String content = "歌单点击分享，复制试听链接后，只留下链接里的那串数字<font color='red'>(仅支持酷我音乐)</font><br>例如:https://m.kuwo.cn/newh5app/playlist_detail/<font color='red'>3141219072</font>?t=plantform&from=ar<br>只需要填写红色数字部分即可";
        dialogBinding.subtitle.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT));
        dialogBinding.searchContent.setHint("请输入歌单ID");
        dialogBinding.positive.setOnClickListener(v -> {
            try {
                String musicName = dialogBinding.searchContent.getText().toString().trim();
                if (musicName.isEmpty()) {
                    useToast("请输入内容！");
                    return;
                }
                
                long t1 = System.currentTimeMillis();
                String musicBindId = dialogBinding.searchContent.getText().toString().trim();
                homeViewModel.getSongDetail(musicBindId);
                bottomSheetDialog.dismiss();
                showLoadingDialog();
                logPerf("Dialog", "提交歌单ID: " + (System.currentTimeMillis() - t1) + "ms");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        dialogBinding.negative.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        logPerf("Dialog", "showBangMusicDialog: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initBangListObserver() {
        logPerf("BangList", "initBangListObserver 开始");
        long start = System.currentTimeMillis();
        
        binding.bangRec.setNestedScrollingEnabled(false);
        
        java.util.concurrent.atomic.AtomicLong callbackStart = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        homeViewModel.getBangList().observe(this, datas -> {
            long duration = System.currentTimeMillis() - callbackStart.get();
            logPerf("BangList", "榜单列表加载回调: " + duration + "ms");
            
            long tt = System.currentTimeMillis();
            BangAdapter bangAdapter = new BangAdapter(datas, requireContext());
            binding.bangRec.setAdapter(bangAdapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
            binding.bangRec.setLayoutManager(gridLayoutManager);
            logPerf("BangList", "设置榜单列表UI: " + (System.currentTimeMillis() - tt) + "ms");
        });
        
        logPerf("BangList", "initBangListObserver: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initHotSongListObserver() {
        logPerf("HotSong", "initHotSongListObserver 开始");
        long start = System.currentTimeMillis();
        
        binding.songRec.setNestedScrollingEnabled(false);
        
        java.util.concurrent.atomic.AtomicLong callbackStart = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        homeViewModel.getSongList().observe(this, datas -> {
            long duration = System.currentTimeMillis() - callbackStart.get();
            logPerf("HotSong", "热门歌曲列表加载回调: " + duration + "ms");
            
            long tt = System.currentTimeMillis();
            SongAdapter songAdapter = new SongAdapter(datas, requireContext(), false);
            binding.songRec.setAdapter(songAdapter);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
            binding.songRec.setLayoutManager(gridLayoutManager);
            initHotSongListOnItemClick(songAdapter);
            logPerf("HotSong", "设置热门歌曲列表UI: " + (System.currentTimeMillis() - tt) + "ms");
        });
        
        logPerf("HotSong", "initHotSongListObserver: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initHotSongListOnItemClick(SongAdapter adapter) {
        if (adapter != null) {
            adapter.setOnItemClickListener((itemView, songBean, position) -> {
                long now = System.currentTimeMillis();
                if (now - lastNavClickTime < 800) return;
                lastNavClickTime = now;
                
                long t1 = System.currentTimeMillis();
                if (requireActivity() instanceof androidx.fragment.app.FragmentActivity) {
                    androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                    androidx.fragment.app.Fragment current = fm.findFragmentById(R.id.fragment_container);
                    MusicFragment musicFragment = MusicFragment.newInstance("playlist", songBean.getSongId(), songBean.getSongName());
                    androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    if (current != null) ft.hide(current);
                    ft.add(R.id.fragment_container, musicFragment, "MusicFragment");
                    ft.addToBackStack(null);
                    ft.commitAllowingStateLoss();
                }
                logPerf("HotSong", "跳转到歌单详情: " + (System.currentTimeMillis() - t1) + "ms");
            });
        }
    }
    
    @Override
    public void onResume() {
        long t1 = System.currentTimeMillis();
        super.onResume();
        logPerf("Lifecycle", "onResume: " + (System.currentTimeMillis() - t1) + "ms [从Fragment创建: " + (System.currentTimeMillis() - fragmentStartTime) + "ms]");
    }
    
    @Override
    public void onPause() {
        long t1 = System.currentTimeMillis();
        super.onPause();
        logPerf("Lifecycle", "onPause: " + (System.currentTimeMillis() - t1) + "ms");
    }
    
    @Override
    public void onDestroyView() {
        long t1 = System.currentTimeMillis();
        super.onDestroyView();
        long totalTime = System.currentTimeMillis() - fragmentStartTime;
        Log.d(TAG, "========================================");
        Log.d(TAG, "Fragment 总生命周期: " + totalTime + "ms");
        Log.d(TAG, "========================================");
        logPerf("Lifecycle", "onDestroyView: " + (System.currentTimeMillis() - t1) + "ms [Fragment总耗时: " + totalTime + "ms]");
    }
}
