package com.linfeng.music.view.fragment;

import android.text.Html;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.linfeng.music.utils.CommonUtils;
import com.linfeng.music.view.base.BaseDialogFragment;


import java.util.List;

public class StorageDialogFragment extends BaseDialogFragment {

    @Override
    protected void initView() {
        binding.lottie.setAnimation("file.json");
        binding.title.setText("存储权限");
        binding.message.setGravity(Gravity.CENTER);
        binding.message.setText(Html.fromHtml("软件需要申请手机<font color='red'>储存权限</font>和<font color='red'>通知权限</font>后，才能够正常使用，此权限用于保存文件与文件操作等。", Html.FROM_HTML_MODE_COMPACT));
        binding.positive.setText("授予");
        binding.negative.setText("取消");
        binding.positive.setOnClickListener(v -> requestPermission());
        binding.negative.setOnClickListener(v -> {
            useToast("取消授权，部分功能将不可用！");
            dismiss();
        });
    }

    /**
     * 请求所需权限 --- 这里需要的权限是外部存储权限
     * <p>
     * 这里使用轮子哥的{@link XXPermissions}库以兼容不同的安卓版本设备
     */
    private void requestPermission() {
        XXPermissions.with(requireActivity())
                .permission(Permission.MANAGE_EXTERNAL_STORAGE, Permission.POST_NOTIFICATIONS)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            useToast("授权成功！");
                            CommonUtils.createDirs(requireContext(), "音频");
                            dismiss();
                        } else {
                            useToast("授权失败，部分功能将不可用！");
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            useToast("请前往设置开启权限！");
                        } else {
                            useToast("授权失败，部分功能将不可用！");
                        }
                    }
                });
    }
}
