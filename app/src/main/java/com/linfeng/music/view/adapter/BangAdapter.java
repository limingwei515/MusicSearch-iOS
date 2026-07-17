package com.linfeng.music.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.linfeng.music.R;
import com.linfeng.music.bean.BangBean;
import com.linfeng.music.utils.ImageLoader;
import com.linfeng.music.view.base.BaseAdapter;
import com.linfeng.music.view.fragment.HomeFragment;
import com.linfeng.music.view.fragment.MusicFragment;

import java.util.List;

public class BangAdapter extends BaseAdapter<BangBean, BangAdapter.MyViewHolder> {
    private final Context context;
    private long lastClickTime = 0;

    public BangAdapter(List<BangBean> datas, Context context) {
        super(datas);
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bang_list, parent, false);
        return new BangAdapter.MyViewHolder(view);
    }

    @Override
    protected void onBindView(MyViewHolder holder, BangBean data, int position) {
        ImageLoader.loadImage(context, data.getBangImgUrl(), holder.image);
        holder.title.setText(data.getBangName());
        holder.itemView.setOnClickListener(view -> {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < 800) return;
            lastClickTime = now;
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();
                androidx.fragment.app.Fragment current = fm.findFragmentById(R.id.fragment_container);
                com.linfeng.music.view.fragment.MusicFragment musicFragment = com.linfeng.music.view.fragment.MusicFragment.newInstance("bang", data.getBangId(), data.getBangName());
                androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                if (current != null) ft.hide(current);
                ft.add(R.id.fragment_container, musicFragment, "MusicFragment");
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
            }
        });
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView image;
        AppCompatTextView title;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
        }
    }
}
