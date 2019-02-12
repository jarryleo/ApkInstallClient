package cn.leo.tcp.file;

import java.util.Map;

/**
 * @author : Jarry Leo
 * @date : 2019/2/11 14:24
 */
public class SimpleReceiveFileListener implements ReceiveFileListener {
    @Override
    public void onNewFile(FileInfo fileInfo, NewFileRequest request) {
        request.accept();
    }

    @Override
    public void onFilesProgress(Map<String, Integer> fileProgressMap) {

    }

    @Override
    public void onFileReceiveFailed(String fileName) {

    }

    @Override
    public void onFileReceiveSuccess(String fileName) {

    }
}
