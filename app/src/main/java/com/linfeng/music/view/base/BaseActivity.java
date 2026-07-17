package com.linfeng.music.view.base;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.linfeng.music.R;

public abstract class BaseActivity<T extends ViewBinding> extends AppCompatActivity {
    protected T binding;
    protected Dialog loadingDialog;
    protected final String TAG = "MusicNotify";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = initViewBinding();
        setContentView(binding.getRoot());
        initStatusBar();
        initActivity();
    }

    protected abstract T initViewBinding();

    protected abstract void initStatusBar();

    protected abstract void initActivity();

    /**
     * 加载弹窗
     * <p>
     * 动画为自定义文件
     */

    protected void showLoadingDialog() {
        loadingDialog = new Dialog(this, R.style.LoadingDialog);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(true);
        loadingDialog.show();
    }

    /**
     * 系统toast
     *
     * @param msg 弹窗的内容
     */
    protected void useToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 判断当前是否是夜间模式
     *
     * @return 是否处于夜间模式
     */
    protected boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 将dp转为px
     */
    protected int dp2px(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                this.getResources().getDisplayMetrics()
        );
    }
}
