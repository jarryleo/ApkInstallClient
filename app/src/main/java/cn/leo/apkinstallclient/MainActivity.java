package cn.leo.apkinstallclient;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cn.leo.permission.PermissionRequest;
import cn.leo.tcp.file.FileInfo;
import cn.leo.tcp.file.FileTransfer;
import cn.leo.tcp.file.IOThreadPool;
import cn.leo.tcp.file.SimpleReceiveFileListener;
import cn.leo.udp.OnDataArrivedListener;
import cn.leo.udp.UdpFrame;
import cn.leo.udp.UdpSender;

/**
 * @author Leo
 */
public class MainActivity extends AppCompatActivity implements OnDataArrivedListener {

    private static final int REQUEST_CODE_APP_INSTALL = 200;
    private RecyclerView mRvList;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UdpSender mSender;
    private FileTransfer mFileTransfer;
    private RvFileListAdapter mAdapter;
    private File mDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        initReceiveFile();
    }

    private void initView() {
        mRvList = findViewById(R.id.rvList);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefresh);
        mRvList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new RvFileListAdapter();
        mRvList.setAdapter(mAdapter);
    }

    private void initEvent() {
        UdpFrame.getListener().subscribe(25536, this);
        mSender = UdpFrame.getSender(25535);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
                IOThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        mSender.sendBroadcast("list".getBytes());
                    }
                });
            }
        });
    }

    @PermissionRequest({Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE})
    private void initReceiveFile() {
        mDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        mFileTransfer = FileTransfer.getInstance();
        mFileTransfer.startReceiver(25537, mDir.getAbsolutePath(), false);
        mFileTransfer.setReceiveFileListener(new SimpleReceiveFileListener() {
            @Override
            public void onFilesProgress(final Map<String, Integer> fileProgressMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setProgress(fileProgressMap);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UdpFrame.getListener().unSubscribe(this);
        mFileTransfer.close();
    }

    @Override
    public void onDataArrived(final byte[] data, String host, int port) {
        mSender.setRemoteHost(host);
        final String s = new String(data, Charset.forName("UTF-8"));
        final List<FileInfo> fileInfoList = JSONObject.parseArray(s, FileInfo.class);
        Collections.sort(fileInfoList, new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo o1, FileInfo o2) {
                return Long.valueOf(o2.getStart()).compareTo(Long.valueOf(o1.getStart()));
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.setData(fileInfoList);
            }
        });
    }

    public void getFile(final String fileName) {
        File file = new File(mDir, fileName);
        if (file.exists()) {
            if (fileName.endsWith("apk")) {
                installApk(fileName);
                return;
            }
            Toast.makeText(this, "文件不是apk，无法安装", Toast.LENGTH_SHORT).show();
            return;
        }
        IOThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                mSender.send(fileName.getBytes());
            }
        });
    }

    private void installApk(String fileName) {
        File file = new File(mDir, fileName);
        //检查权限
        boolean b = checkPermission();
        if (b) {
            //安装apk
            install(file.getAbsolutePath());
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean hasInstallPermission = isHasInstallPermissionWithO(this);
            if (!hasInstallPermission) {
                startInstallPermissionSettingActivity(this);
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isHasInstallPermissionWithO(Context context) {
        if (context == null) {
            return false;
        }
        return context.getPackageManager().canRequestPackageInstalls();
    }

    /**
     * 开启设置安装未知来源应用权限界面
     *
     * @param context
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity(Context context) {
        if (context == null) {
            return;
        }
        Uri packageUri = Uri.parse("package:" + getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_APP_INSTALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_APP_INSTALL) {

            }
        }
    }

    private void install(String filePath) {
        File apkFile = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(
                    this
                    , "cn.leo.apkinstallclient.fileProvider"
                    , apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }
}
