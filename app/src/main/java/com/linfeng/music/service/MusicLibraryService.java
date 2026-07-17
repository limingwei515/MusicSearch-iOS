package com.linfeng.music.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.linfeng.music.R;
import com.linfeng.music.bean.MusicBean;
import com.linfeng.music.view.activity.MusicActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Media3 音乐播放服务
 *
 * 核心设计：
 * - ExoPlayer 负责底层播放
 * - 完整播放列表存储在 service 内（playbackQueue）
 * - 上一首/下一首 由 ViewModel 通过 skipListener 回调触发
 * - MediaSession 通知栏控制也走 skipListener
 */
public class MusicLibraryService extends Service {

    private static final String TAG = "MusicLibraryService";
    private static final boolean DEBUG = false;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "music_channel_v2";

    private ExoPlayer player;
    private MediaSessionManager mediaSessionManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ======== 播放队列（关键：存全部歌曲）========
    private final List<MusicBean> playbackQueue = new CopyOnWriteArrayList<>();
    private int currentQueueIndex = 0;
    private MusicBean currentMusicData;

    // ======== 回调 ========
    private PlaybackListener playbackListener;
    private ProgressCallback progressCallback;
    // 切歌回调：服务需要切歌时通知 ViewModel
    private SkipCallback skipCallback;

    // ======== Binder ========
    public class MusicBinder extends Binder {
        public MusicLibraryService getService() {
            return MusicLibraryService.this;
        }
    }

    private final IBinder binder = new MusicBinder();

    // ======== 生命周期 ========
    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
        createNotificationChannel();
        initPlayer();
    }

    private void initPlayer() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    handlePlaybackEnded();
                }
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(state);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    handler.post(progressRunnable);
                } else {
                    handler.removeCallbacks(progressRunnable);
                }
                if (playbackListener != null) {
                    playbackListener.onIsPlayingChanged(isPlaying);
                }
                updateNotification();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (playbackListener != null) {
                    playbackListener.onPlayerError(error.getMessage());
                }
            }
        });

        mediaSessionManager = new MediaSessionManager(this, player, new MediaSessionManager.SkipHandler() {
            @Override
            public void onSkipToNext() {
                skipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                skipToPrevious();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int actionCode = intent.getIntExtra("action_code", -1);
            if (actionCode == 0) {
                skipToPrevious();
            } else if (actionCode == 1) {
                // 播放/暂停
                if (player.isPlaying()) pause();
                else resume();
            } else if (actionCode == 2) {
                skipToNext();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log("onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        handler.removeCallbacksAndMessages(null);
        if (mediaSessionManager != null) {
            mediaSessionManager.release();
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    // ======== 播放控制 ========
    /**
     * 播放一首新歌（保存完整播放列表）
     */
    public void play(String url, MusicBean musicBean, List<MusicBean> queue, int index) {
        if (url == null || url.isEmpty()) {
            if (playbackListener != null) playbackListener.onPlayerError("播放地址无效");
            return;
        }

        log("play: " + musicBean.getName() + " (queue size: " + (queue == null ? 0 : queue.size()) + ")");

        // 更新队列：始终保存全部歌曲
        if (queue != null && !queue.isEmpty()) {
            playbackQueue.clear();
            playbackQueue.addAll(queue);
            currentQueueIndex = Math.min(index, queue.size() - 1);
        } else {
            playbackQueue.clear();
            playbackQueue.add(musicBean);
            currentQueueIndex = 0;
        }
        currentMusicData = musicBean;

        // 构建 MediaItem 并播放
        MediaItem item = buildMediaItem(url, musicBean);
        player.setMediaItem(item);
        player.prepare();
        player.setPlayWhenReady(true);
        showNotification();
    }

    public void pause() {
        player.setPlayWhenReady(false);
        updateNotification();
    }

    public void resume() {
        player.setPlayWhenReady(true);
        updateNotification();
    }

    public void stop() {
        player.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    public void seekTo(int positionMs) {
        player.seekTo(positionMs);
    }

    /**
     * 下一首（由 ViewModel 或通知栏触发）
     */
    public void skipToNext() {
        log("skipToNext: queueSize=" + playbackQueue.size() + ", index=" + currentQueueIndex);
        if (playbackQueue.isEmpty()) {
            log("skipToNext: queue empty");
            return;
        }
        // 通知 ViewModel 由它处理切歌（获取 URL 后再播放）
        if (skipCallback != null) {
            int nextIndex = currentQueueIndex + 1;
            if (nextIndex >= playbackQueue.size()) {
                nextIndex = 0; // 循环
            }
            skipCallback.onSkipTo(nextIndex, playbackQueue.get(nextIndex));
        } else {
            log("skipToNext: skipCallback is null!");
        }
    }

    /**
     * 上一首（由 ViewModel 或通知栏触发）
     */
    public void skipToPrevious() {
        log("skipToPrevious: queueSize=" + playbackQueue.size() + ", index=" + currentQueueIndex);
        if (playbackQueue.isEmpty()) {
            log("skipToPrevious: queue empty");
            return;
        }
        // 如果当前位置 > 3秒，重新播放当前；否则切到上一首
        if (player.getCurrentPosition() > 3000) {
            player.seekTo(0);
            updateNotification();
        } else if (skipCallback != null) {
            int prevIndex = currentQueueIndex - 1;
            if (prevIndex < 0) {
                prevIndex = playbackQueue.size() - 1;
            }
            skipCallback.onSkipTo(prevIndex, playbackQueue.get(prevIndex));
        }
    }

    public int getCurrentPosition() {
        return player != null ? (int) player.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return player != null ? (int) Math.max(player.getDuration(), 0) : 0;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    // ======== 队列访问 ========
    public List<MusicBean> getPlaybackQueue() {
        return new ArrayList<>(playbackQueue);
    }

    public int getCurrentQueueIndex() {
        return currentQueueIndex;
    }

    public MusicBean getCurrentMusicData() {
        return currentMusicData;
    }

    public void updateCurrentSong(MusicBean bean, int index) {
        this.currentMusicData = bean;
        this.currentQueueIndex = index;
    }

    // ======== 回调设置 ========
    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * 设置切歌回调：当 skipToNext/skipToPrevious 被调用时通知 ViewModel
     */
    public void setSkipCallback(SkipCallback callback) {
        this.skipCallback = callback;
    }

    // ======== 进度更新 ========
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                int current = getCurrentPosition();
                int duration = getDuration();
                if (progressCallback != null && duration > 0) {
                    progressCallback.onProgressUpdate(current, duration);
                }
                updateNotification();
            }
            handler.postDelayed(this, 1000);
        }
    };

    // ======== 内部处理 ========
    private void handlePlaybackEnded() {
        // 自动切下一首
        if (skipCallback != null && !playbackQueue.isEmpty()) {
            int nextIndex = currentQueueIndex + 1;
            if (nextIndex >= playbackQueue.size()) {
                nextIndex = 0;
            }
            skipCallback.onSkipTo(nextIndex, playbackQueue.get(nextIndex));
        }
    }

    private MediaItem buildMediaItem(String url, MusicBean bean) {
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .setTitle(bean.getName())
                .setArtist(bean.getArtist());
        if (bean.getAlbumIcon() != null) {
            meta.setArtworkUri(android.net.Uri.parse(bean.getAlbumIcon()));
        }
        return new MediaItem.Builder()
                .setUri(url)
                .setMediaId(bean.getId())
                .setMediaMetadata(meta.build())
                .build();
    }

    // ======== 通知栏 ========
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("音乐播放控制");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private void showNotification() {
        if (currentMusicData == null) return;
        updateNotification();
    }

    private void updateNotification() {
        if (currentMusicData == null || player == null) return;

        boolean playing = player.isPlaying();
        int current = getCurrentPosition();
        int duration = getDuration();
        if (duration <= 0) duration = 1;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentMusicData.getName())
                .setContentText(formatTime(current) + " / " + formatTime(duration))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSessionManager.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(R.drawable.icon_skip_previous, "上一首",
                        createActionIntent(0))
                .addAction(playing ? R.drawable.icon_pause : R.drawable.icon_play_arrow,
                        playing ? "暂停" : "播放",
                        createActionIntent(1))
                .addAction(R.drawable.icon_skip_next, "下一首",
                        createActionIntent(2))
                .setOngoing(playing)
                .setProgress(duration, playing ? current : 0, !playing)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MusicActivity.class),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        if (currentMusicData.getAlbumIcon() != null) {
            Glide.with(this).asBitmap().load(currentMusicData.getAlbumIcon())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap r, @Nullable Transition<? super Bitmap> t) {
                            builder.setLargeIcon(r);
                            notifyNotification(builder.build());
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable p) {
                            notifyNotification(builder.build());
                        }
                    });
        } else {
            notifyNotification(builder.build());
        }
    }

    private void notifyNotification(android.app.Notification notification) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent createActionIntent(int code) {
        Intent intent = new Intent(this, MusicLibraryService.class);
        intent.putExtra("action_code", code);
        return PendingIntent.getService(this, code, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void log(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }

    // ======== 内部类：MediaSession 管理 ========
    private static class MediaSessionManager {
        private final android.support.v4.media.session.MediaSessionCompat mediaSession;
        private final Player player;

        interface SkipHandler {
            void onSkipToNext();
            void onSkipToPrevious();
        }

        MediaSessionManager(Context context, Player player, SkipHandler skipHandler) {
            this.player = player;
            ComponentName name = new ComponentName(context, MusicLibraryService.class);
            mediaSession = new android.support.v4.media.session.MediaSessionCompat(context, "MusicLibrary", name, null);
            mediaSession.setActive(true);

            mediaSession.setCallback(new android.support.v4.media.session.MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    player.setPlayWhenReady(true);
                    updateState(android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING);
                }

                @Override
                public void onPause() {
                    player.setPlayWhenReady(false);
                    updateState(android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED);
                }

                @Override
                public void onSkipToNext() {
                    skipHandler.onSkipToNext();
                }

                @Override
                public void onSkipToPrevious() {
                    skipHandler.onSkipToPrevious();
                }

                @Override
                public void onSeekTo(long pos) {
                    player.seekTo(pos);
                }
            });
            updateState(android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED);
        }

        private void updateState(int state) {
            try {
                mediaSession.setPlaybackState(
                        new android.support.v4.media.session.PlaybackStateCompat.Builder()
                                .setActions(
                                        android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
                                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
                                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                                | android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO)
                                .setState(state, player.getCurrentPosition(), 1.0f)
                                .build()
                );
            } catch (Exception ignored) {}
        }

        android.support.v4.media.session.MediaSessionCompat.Token getSessionToken() {
            return mediaSession.getSessionToken();
        }

        void release() {
            mediaSession.release();
        }
    }

    // ======== 回调接口 ========
    public interface PlaybackListener {
        void onPlaybackStateChanged(int state);
        void onIsPlayingChanged(boolean isPlaying);
        void onPlaybackCompleted();
        void onPlayerError(String message);
    }

    public interface ProgressCallback {
        void onProgressUpdate(int currentMs, int durationMs);
    }

    /**
     * 切歌回调：服务需要切歌时调用，ViewModel 实现此接口来获取 URL 并播放
     */
    public interface SkipCallback {
        void onSkipTo(int nextIndex, MusicBean nextSong);
    }
}
