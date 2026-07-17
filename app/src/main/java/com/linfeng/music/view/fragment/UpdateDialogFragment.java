package com.linfeng.music.view.fragment;

import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import com.linfeng.music.utils.ConfigsRepository;
import com.linfeng.music.view.base.BaseDialogFragment;

import java.util.Map;
import java.util.Objects;

public class UpdateDialogFragment extends BaseDialogFragment {
    @Override
    protected void initView() {
        Map<String, String> configs = (Map<String, String>) ConfigsRepository.getConfigs().get("软件更新控制");

        binding.lottie.setAnimation("update.json");
        binding.title.setText(configs.get("弹窗标题"));
        binding.message.setText(Html.fromHtml(configs.get("弹窗内容"), Html.FROM_HTML_MODE_COMPACT));
        binding.positive.setText("更新");
        binding.negative.setText("取消");

        binding.positive.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(configs.get("更新地址")));
            startActivity(intent);
        });
        binding.negative.setOnClickListener(v -> {
            if (Objects.equals(configs.get("强制更新"), "开启")) {
                System.exit(0);
            }
            dismiss();
        });
    }
}
