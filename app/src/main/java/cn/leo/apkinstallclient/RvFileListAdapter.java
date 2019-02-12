package cn.leo.apkinstallclient;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.leo.tcp.file.FileInfo;

/**
 * @author : Jarry Leo
 * @date : 2019/2/12 9:58
 */
public class RvFileListAdapter extends RecyclerView.Adapter<RvFileListHolder> {
    private List<FileInfo> mList = new ArrayList<>();

    public void setData(List<FileInfo> data) {
        mList.clear();
        mList.addAll(data);
        notifyDataSetChanged();
    }

    public void setProgress(Map<String, Integer> fileProgressMap) {
        for (Map.Entry<String, Integer> entry : fileProgressMap.entrySet()) {
            String fileName = entry.getKey();
            for (int i = 0; i < mList.size(); i++) {
                FileInfo fileInfo = mList.get(i);
                String name = fileInfo.getFileName();
                if (name.equals(fileName)) {
                    fileInfo.setType(1);
                    fileInfo.setPartIndex(entry.getValue());
                    notifyItemChanged(i);
                }
            }
        }
    }

    @NonNull
    @Override
    public RvFileListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rv_file_list, parent, false);
        return new RvFileListHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull RvFileListHolder holder, int position) {
        FileInfo fileInfo = mList.get(position);
        holder.setData(fileInfo);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


}
