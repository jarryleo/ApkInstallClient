package cn.leo.tcp.file;

import java.util.Map;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 14:33
 */
public interface ReceiveFileListener {
    /**
     * 新文件传输请求
     *
     * @param request
     */
    void onNewFile(FileInfo fileInfo, NewFileRequest request);

    /**
     * 文件传输进度
     *
     * @param fileProgressMap 文件名对应进度集合
     */
    void onFilesProgress(Map<String, Integer> fileProgressMap);

    /**
     * 文件传输出错
     */
    void onFileReceiveFailed(String fileName);

    /**
     * 接受文件成功
     */
    void onFileReceiveSuccess(String fileName);
}
