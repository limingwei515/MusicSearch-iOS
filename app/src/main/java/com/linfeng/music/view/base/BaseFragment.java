package com.linfeng.music.view.base;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.linfeng.music.R;
import com.linfeng.music.utils.PreferencesManager;

public abstract class BaseFragment<T extends ViewBinding> extends Fragment {
    protected T binding;
    protected Dialog loadingDialog;

    protected PreferencesManager instance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 初始化 ViewBinding
        binding = initViewBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        instance = PreferencesManager.getInstance(requireContext());
        initStatusBar();
        initFragment();
    }

    /**
     * 判断当前是否是夜间模式
     *
     * @return true为夜间模式，false为日间模式
     */
    protected boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 封装{@code Toast}以方便调用
     *
     * @param msg 提示的内容
     */
    protected void useToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * dp转px
     *
     * @param dp 需要转换的dp
     * @return 转换后的px
     */
    protected int dp2px(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                this.getResources().getDisplayMetrics()
        );
    }

    /**
     * 打开加载中弹窗
     * <p>
     * 用途：用于进行异步操作时给用户提供UI反馈
     */

    protected void showLoadingDialog() {
        loadingDialog = new Dialog(requireContext(), R.style.LoadingDialog);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(true);
        loadingDialog.show();
    }

    protected abstract T initViewBinding(LayoutInflater inflater, @Nullable ViewGroup container);

    protected abstract void initStatusBar();

    protected abstract void initFragment();
}
