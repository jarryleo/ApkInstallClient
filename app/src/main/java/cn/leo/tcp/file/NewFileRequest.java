package cn.leo.tcp.file;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 14:35
 */
public interface NewFileRequest {
    /**
     * 接收文件
     */
    void accept();

    /**
     * 拒绝文件
     */
    void denied();
}
