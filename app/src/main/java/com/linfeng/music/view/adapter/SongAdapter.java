package com.linfeng.music.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linfeng.music.R;
import com.linfeng.music.bean.SongBean;
import com.linfeng.music.utils.CommonUtils;
import com.linfeng.music.utils.ImageLoader;
import com.linfeng.music.utils.PreferencesManager;
import com.linfeng.music.view.base.BaseAdapter;

import java.util.List;

public class SongAdapter extends BaseAdapter<SongBean, SongAdapter.MyViewHolder> {
    private Context context;
    private boolean mySongs;
    private long lastClickTime = 0;
    private OnItemClickListener listener;

    public SongAdapter(List<SongBean> datas, Context context, boolean mySongs) {
        super(datas);
        this.context = context;
        this.mySongs = mySongs;
    }

    @Override
    protected void onBindView(MyViewHolder holder, SongBean data, int position) {
        ImageLoader.loadImage(context, data.getSongImgUrl(), holder.image);
        holder.title.setText(data.getSongName());
        String listenCnt = data.getListencnt();
        if (listenCnt == null || listenCnt.trim().isEmpty()) {
            listenCnt = "0";
        }
        holder.listencnt.setText(CommonUtils.formatNumber(listenCnt));

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 800) return;
                lastClickTime = now;
                listener.onItemClick(holder.itemView, data, position);
            });
        }

        if (mySongs) {
            holder.itemView.setOnLongClickListener(view -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle("温馨提示")
                        .setMessage("确定要删除该歌单吗？")
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            PreferencesManager instance = PreferencesManager.getInstance(context);
                            List<SongBean> mySongsList = instance.getList(context, "MySongsList", SongBean.class);
                            mySongsList.remove(position);
                            instance.putList(context, "MySongsList", mySongsList);
                            setData(mySongsList);
                            notifyItemRemoved(position);
                        })
                        .setNegativeButton("取消", null);
                builder.show();
                return true;
            });
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_list, parent, false);
        return new MyViewHolder(view);
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, SongBean songBean, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public AppCompatImageView image;
        public AppCompatTextView title;
        public MaterialButton listencnt;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            listencnt = itemView.findViewById(R.id.listencnt);
        }
    }

}
