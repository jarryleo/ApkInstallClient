package cn.leo.tcp.file;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 9:27
 */
interface Constant {
    /**
     * 文件多线程传输分段大小
     */
    int FILE_PART_SIZE = 10 * 1024 * 1024;
    /**
     * 单文件最多几个线程传输
     */
    int FILE_PART_NUM = 3;
    /**
     * 文件缓冲区大小
     */
    int BUFFER_SIZE = 1024;
    /**
     * 连接类型为请求传输文件
     */
    int CONNECTION_TYPE_REQUEST = 1;
    /**
     * 连接类型为多线程传输
     */
    int CONNECTION_TYPE_THREAD = 2;
}
