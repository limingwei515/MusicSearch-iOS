package com.linfeng.music.view.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.app.Activity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.linfeng.music.R;
import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.database.entity.FavoriteEntity;
import com.linfeng.music.repository.FavoriteRepository;
import com.linfeng.music.utils.ImageLoader;
import com.linfeng.music.view.base.BaseAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MusicAdapter extends BaseAdapter<MusicBean, MusicAdapter.MyViewHolder> {

    private final Context context;
    private OnItemClickListener listener;
    private OnDownloadClickListener downloadListener;
    private final boolean hasIcon;
    private final FavoriteRepository favoriteRepository;
    private final CompositeDisposable disposable = new CompositeDisposable();
    // 本地缓存：已收藏的音乐ID集合
    private final Set<String> likedIds = new HashSet<>();

    public MusicAdapter(List<MusicBean> datas, Context context, boolean hasIcon) {
        super(datas);
        this.context = context;
        this.hasIcon = hasIcon;
        this.favoriteRepository = FavoriteRepository.getInstance(context);
        // 初始化时加载所有收藏ID
        loadLikedIds();
    }

    private void loadLikedIds() {
        disposable.add(
            favoriteRepository.getAllFavoritesAsMusicBeans()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(beans -> {
                    likedIds.clear();
                    for (MusicBean b : beans) likedIds.add(b.getId());
                    notifyDataSetChanged();
                }, error -> {})
        );
    }

    @Override
    protected void onBindView(MyViewHolder holder, MusicBean data, int position) {
        if (hasIcon) {
            ImageLoader.loadImage(context, data.getAlbumIcon(), holder.albumIcon, R.drawable.icon_gedan);
        } else {
            holder.albumIcon.setVisibility(View.GONE);
            holder.number.setVisibility(View.VISIBLE);
            holder.number.setText(String.valueOf(position + 1));
        }
        holder.name.setText(data.getName());
        holder.artist.setText(data.getArtist());

        boolean isLiked = likedIds.contains(data.getId());
        data.setLike(isLiked);
        updateLikeIcon(holder.like, isLiked);

        holder.like.setOnClickListener(v -> toggleLike(holder, data));

        // 点击整个 item → 播放音乐
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onItemClick(holder, data, position));
        }
        // 点击下载按钮 → 触发下载回调
        if (downloadListener != null) {
            holder.download.setOnClickListener(v -> downloadListener.onDownloadClick(data));
        }
    }

    private void toggleLike(MyViewHolder holder, MusicBean data) {
        boolean willBeLiked = !likedIds.contains(data.getId());

        // 先更新本地缓存和UI（乐观更新）
        if (willBeLiked) {
            likedIds.add(data.getId());
        } else {
            likedIds.remove(data.getId());
        }
        data.setLike(willBeLiked);
        updateLikeIcon(holder.like, willBeLiked);

        // 再更新数据库
        disposable.add(
            favoriteRepository.toggleFavorite(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {}, error -> {
                    // 失败则回滚UI
                    if (willBeLiked) {
                        likedIds.remove(data.getId());
                        data.setLike(false);
                    } else {
                        likedIds.add(data.getId());
                        data.setLike(true);
                    }
                    notifyItemChanged(getAllData().indexOf(data));
                })
        );
    }

    private void updateLikeIcon(MaterialButton likeBtn, boolean isLiked) {
        if (isLiked) {
            likeBtn.setIconResource(R.drawable.icon_like_select);
            likeBtn.setIconSize(dp2px(20));
            likeBtn.setIconTint(ColorStateList.valueOf(Color.parseColor("#ff4e00")));
        } else {
            likeBtn.setIconResource(R.drawable.icon_like);
            likeBtn.setIconSize(dp2px(24));
            TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorOnBackground, value, true);
            likeBtn.setIconTint(ColorStateList.valueOf(value.data));
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music_list, parent, false);
        return new MyViewHolder(view);
    }

    // 外部调用刷新收藏状态
    public void refreshLikedState() {
        loadLikedIds();
    }

    public interface OnItemClickListener {
        void onItemClick(MyViewHolder holder, MusicBean musicBean, Integer position);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(MusicBean musicBean);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView albumIcon;
        AppCompatTextView name, artist, number;
        public MaterialButton download, like;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            albumIcon = itemView.findViewById(R.id.album_icon);
            name = itemView.findViewById(R.id.name);
            artist = itemView.findViewById(R.id.artist);
            download = itemView.findViewById(R.id.download);
            number = itemView.findViewById(R.id.number);
            like = itemView.findViewById(R.id.like);
        }
    }

    protected int dp2px(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
