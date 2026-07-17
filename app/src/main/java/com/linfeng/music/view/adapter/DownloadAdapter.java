package com.linfeng.music.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.task.DownloadTask;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.haozhang.lib.SlantedTextView;
import com.linfeng.music.R;
import com.linfeng.music.utils.Mp3TagUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.MyViewHolder> {
    private Context context;
    private List<DownloadEntity> list;
    private Map<Long, Integer> itemIndex = new HashMap<>();

    public DownloadAdapter(Context context, List<DownloadEntity> list) {
        this.context = context;
        this.list = list;
        Iterator<DownloadEntity> iterator = list.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            itemIndex.put(iterator.next().getId(), i);
            i++;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.item_download_list, parent, false);
        return new MyViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DownloadEntity entity = list.get(position);
        holder.name.setText(entity.getFileName());
        // 渲染基本信息
        String str = entity.getStr();
        String[] split = str.split("\\|");
        String icon = split[0];
        String tonal = split[1];

        // 写入ID3标签（仅在文件存在且未写入时尝试，防止重复写入）
        File file = new File(entity.getFilePath());
        if (entity.isComplete() && file.exists()) {
            // 将耗时操作移到后台线程执行
            new Thread(() -> {
                try {
                    // 用mp3agic判断是否已写入封面
                    boolean hasCover = false;
                    com.mpatric.mp3agic.Mp3File mp3file = new com.mpatric.mp3agic.Mp3File(file.getAbsolutePath());
                    if (mp3file.hasId3v2Tag() && mp3file.getId3v2Tag().getAlbumImage() != null) {
                        hasCover = true;
                    }
                    if (!hasCover) {
                        HashMap<String, String> metaMap = new HashMap<>();
                        metaMap.put("title", entity.getFileName());
                        metaMap.put("artist", "");
                        metaMap.put("album", "");
                        metaMap.put("albumArtist", "");
                        metaMap.put("composer", "");
                        metaMap.put("genre", "");
                        metaMap.put("coverUrl", icon); // 用extendField里的albumIcon
                        com.linfeng.music.utils.Mp3TagUtils.writeTags(file.getAbsolutePath(), metaMap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // 设置软件品质斜标签
        holder.sv.setText(tonal.substring(0, 2));
        // 渲染软件图标（使用 Glide 缓存机制）
        Glide.with(context)
                .load(icon)
                .placeholder(R.mipmap.ic_launcher)
                .transition(DrawableTransitionOptions.withCrossFade(300)) // 增加过渡动画时长
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 启用磁盘缓存
                .skipMemoryCache(false) // 启用内存缓存
                .into(holder.icon);
        if (entity.isComplete()) {
            holder.itemCompleteBind(entity);
        } else {
            holder.itemNoCompleteBind(entity);
        }

        // 长按弹出删除提示框
        holder.itemView.setOnLongClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("温馨提示")
                    .setMessage("是否删除该音乐？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 删除下载任务和文件
                        Aria.download(context).load(entity.getId()).cancel(true);
                        if (file.exists()) file.delete();
                        list.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, list.size() - position);
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public void clearFile() {
        list.clear();
        itemIndex.clear();
        notifyDataSetChanged();
    }

    private synchronized int indexItem(long id) {
        for (long id2 : itemIndex.keySet()) {
            if (id2 == id) {
                return itemIndex.get(id2);
            }
        }
        return -1;
    }

    public synchronized void updateState(DownloadTask task) {
        DownloadEntity entity = task.getDownloadEntity();
        int indexItem = indexItem(entity.getId());
        if (indexItem != -1) {
            // 这里是aria的缺陷应该，手动赋值给速度变程转换后已完成的进度吧，后续重写aria的时候看一下
            entity.setConvertSpeed(task.getConvertCurrentProgress());
            list.set(indexItem, entity);
            // 只有当running状态不闪烁
            if (entity.getState() == 4) {
                notifyItemChanged(indexItem, entity);
            } else {
                notifyItemChanged(indexItem);
            }
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        LinearProgressIndicator progress;
        AppCompatTextView name, progressText;
        AppCompatImageView icon;
        MaterialButton button;
        SlantedTextView sv;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            progress = itemView.findViewById(R.id.progress);
            name = itemView.findViewById(R.id.name);
            icon = itemView.findViewById(R.id.icon);
            progressText = itemView.findViewById(R.id.progress_text);
            button = itemView.findViewById(R.id.button);
            sv = itemView.findViewById(R.id.sv);
        }

        public void itemCompleteBind(DownloadEntity entity) {
            button.setText("打开");
            progress.setVisibility(View.GONE);
            String fileSize = entity.getConvertFileSize();
            progressText.setText(String.format("%s", fileSize));
            // 打开音乐文件
            button.setOnClickListener(v -> {
                // 创建文件的Uri
                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", new File(entity.getFilePath()));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "*/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // 使用 Intent.chooser 弹出选择器
                Intent chooserIntent = Intent.createChooser(intent, "选择打开应用");
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(chooserIntent);
                } else {
                    // 没有可用的应用程序时提示用户
                    Toast.makeText(context, "没有可打开播放音频的应用", Toast.LENGTH_SHORT).show();
                }
            });

        }

        public void itemNoCompleteBind(DownloadEntity entity) {
            progress.setProgress(entity.getPercent());
            progressText.setText(String.format("%s / %s", entity.getConvertSpeed(), entity.getConvertFileSize()));
            int state = entity.getState();
            switch (state) {
                case -1:
                case 0:
                    button.setText("重试");
                    button.setOnClickListener(v -> {
                        Aria.download(this).load(entity.getId())
                                .ignoreCheckPermissions()
                                .resume(true);
                    });
                    break;
                case 1:
                    button.setText("打开");
                    break;
                case 2:
                    button.setText("继续");
                    button.setOnClickListener(v -> {
                        Aria.download(this).load(entity.getId())
                                .ignoreCheckPermissions()
                                .resume();
                    });
                    break;
                case 3:
                    button.setText("等待");
                    button.setOnClickListener(v -> {
                        Aria.download(this).load(entity.getId())
                                .ignoreCheckPermissions()
                                .resume();
                    });
                    break;
                case 4:
                    button.setText("暂停");
                    button.setOnClickListener(v -> {
                        Aria.download(this).load(entity.getId())
                                .ignoreCheckPermissions()
                                .stop();
                    });
                    break;
                case 5:
                    button.setText("等待");
                    break;
                case 6:
                    button.setText("等待");
                    break;
            }
        }
    }
}
