package cn.leo.tcp.file;

import java.io.File;
import java.util.List;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 12:06
 */
public abstract class FileTransfer {
    /**
     * 创建文件接收者
     *
     * @param port   从哪个端口接收
     * @param dir    接收后保存在哪个目录
     * @param rename 如果目录存在同名文件，是否自动命名，false覆盖
     */
    public abstract void startReceiver(int port, String dir, boolean rename);

    /**
     * 设置文件接受回调
     *
     * @param receiveFileListener
     */
    public abstract void setReceiveFileListener(ReceiveFileListener receiveFileListener);

    /**
     * 发送文件列表
     *
     * @param fileList 要发送的文件列表
     * @param host     目标地址
     * @param port     目标端口
     */
    public abstract void sendFiles(List<File> fileList, String host, int port);

    /**
     * 发送文件
     *
     * @param file 要发送的文件
     * @param host 目标地址
     * @param port 目标端口
     */
    public abstract void sendFile(File file, String host, int port);

    /**
     * 设置文件发送回调
     *
     * @param sendFileListener
     */
    public abstract void setSendFileListener(SendFileListener sendFileListener);


    public static FileTransfer getInstance() {
        FileTransferImpl fileTransfer = new FileTransferImpl();
        IOThreadPool.execute(fileTransfer);
        return fileTransfer;
    }

    public abstract void close();
}
