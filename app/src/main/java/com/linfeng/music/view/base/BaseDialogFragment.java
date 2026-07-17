package com.linfeng.music.view.base;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linfeng.music.R;
import com.linfeng.music.databinding.DialogLinfengBinding;

import java.util.Objects;

public abstract class BaseDialogFragment extends DialogFragment {
    protected DialogLinfengBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View root = getLayoutInflater().inflate(R.layout.dialog_linfeng, null);
        binding = DialogLinfengBinding.bind(root);
        initView();
        return builder.setView(root).create();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 正确位置：在对话框已创建后设置窗口属性
        // 设置弹窗的宽度为用户设备宽度的80%
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
            window.setAttributes(params);
        }
    }

    protected abstract void initView();

    /**
     * 封装{@code Toast}以方便调用
     *
     * @param msg 提示的内容
     */
    protected void useToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

}
