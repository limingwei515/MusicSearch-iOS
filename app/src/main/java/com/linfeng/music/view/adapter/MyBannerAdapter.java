package com.linfeng.music.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.linfeng.music.R;
import com.linfeng.music.bean.BannerBean;
import com.linfeng.music.utils.ImageLoader;
import com.youth.banner.adapter.BannerAdapter;

import java.util.List;

public class MyBannerAdapter extends BannerAdapter<BannerBean, MyBannerAdapter.MyViewHolder> {

    private final Context context;

    public MyBannerAdapter(List<BannerBean> datas, Context context) {
        super(datas);
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindView(MyViewHolder holder, BannerBean data, int position, int size) {
        ImageLoader.loadImage(context, data.getImageUrl(), holder.image);
        holder.title.setText(data.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (!data.getAddressUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data.getAddressUrl()));
                context.startActivity(intent);
            }
        });
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView image;
        AppCompatTextView title;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
        }
    }
}
