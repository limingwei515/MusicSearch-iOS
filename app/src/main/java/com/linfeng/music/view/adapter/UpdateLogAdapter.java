package com.linfeng.music.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linfeng.music.R;
import com.linfeng.music.utils.CommonUtils;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UpdateLogAdapter extends RecyclerView.Adapter<UpdateLogAdapter.MyViewHolder> {
    private Context context;
    private List<Map<String, String>> logList;

    public UpdateLogAdapter(Context context) throws JSONException {
        this.context = context;
        // 获取本地的更新日志并解析到本地数据
        String updateLog = CommonUtils.getFileContentFromAssets(context, "updateLog.json");
        Gson gson = new Gson();
        // 定义类型令牌以指定泛型类型
        Type type = new TypeToken<List<Map<String, String>>>() {
        }.getType();
        logList = gson.fromJson(updateLog, type);
        Collections.reverse(logList);// 反转list
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.item_update_log, parent, false);
        return new MyViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Map<String, String> map = logList.get(position);
        holder.version.setText(String.format("版本 v%s", map.get("version")));
        holder.time.setText(String.format("更新日期：%s", map.get("time")));
        holder.content.setText(map.get("content"));
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private AppCompatTextView time, version, content;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.time);
            version = itemView.findViewById(R.id.version);
            content = itemView.findViewById(R.id.content);
        }
    }
}
