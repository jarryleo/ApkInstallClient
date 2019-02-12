package cn.leo.apkinstallclient;

import android.content.Context;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import cn.leo.tcp.file.FileInfo;

/**
 * @author : Jarry Leo
 * @date : 2019/2/12 9:59
 */
public class RvFileListHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mTvFileName;
    private final TextView mTvFileSize;
    private final ProgressBar mProgressBar;
    private final TextView mTvProgress;

    public RvFileListHolder(View itemView) {
        super(itemView);
        mTvFileName = itemView.findViewById(R.id.tvFileName);
        mTvFileSize = itemView.findViewById(R.id.tvFileSize);
        mProgressBar = itemView.findViewById(R.id.progressBar);
        mTvProgress = itemView.findViewById(R.id.tvProgress);
        itemView.setOnClickListener(this);
    }

    public void setData(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName();
        mTvFileName.setText(fileName);
        mTvFileSize.setText("文件大小：" + fileInfo.getFileSize());
        int type = fileInfo.getType();
        int percent = fileInfo.getPartIndex();
        if (checkFileExits(fileName)) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mTvProgress.setVisibility(View.VISIBLE);
            mTvProgress.setText("已下载");
            return;
        }
        if (type == 0) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mTvProgress.setVisibility(View.INVISIBLE);
        } else {
            mTvProgress.setVisibility(View.VISIBLE);
            if (percent == 100) {
                mTvProgress.setText("下载完成");
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                mProgressBar.setVisibility(View.VISIBLE);
                mTvProgress.setText(String.valueOf(percent) + "%");
            }
        }
    }

    private boolean checkFileExits(String fileName) {
        File dir = itemView.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, fileName);
        return file.exists();
    }

    @Override
    public void onClick(View v) {
        if (mTvProgress.getVisibility() == View.VISIBLE) {
            String s = mTvProgress.getText().toString();
            if (!"已下载".equals(s)) {
                return;
            }
        }
        Context context = v.getContext();
        if (context instanceof MainActivity) {
            String s = mTvFileName.getText().toString();
            ((MainActivity) context).getFile(s);
            if (mTvProgress.getVisibility() != View.VISIBLE) {
                mTvProgress.setVisibility(View.VISIBLE);
                mTvProgress.setText("开始下载");
            }
        }
    }
}
