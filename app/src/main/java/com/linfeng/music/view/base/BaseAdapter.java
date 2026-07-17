package com.linfeng.music.view.base;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<T> dataList = new ArrayList<>();

    public BaseAdapter(List<T> datas) {
        setData(datas);
    }

    public void setData(List<T> list) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }
        dataList.clear();
        if (list != null) {
            dataList.addAll(list);
        }
    }

    public List<T> getAllData() {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }
        return dataList;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        onBindView(holder, dataList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    protected abstract void onBindView(VH holder, T data, int position);


}
