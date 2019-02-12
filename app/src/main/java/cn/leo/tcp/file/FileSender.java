package cn.leo.tcp.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 8:50
 */
class FileSender {
    private static SendFileListener sendFileListener;

    private FileSender() {

    }

    static void setSendFileListener(SendFileListener listener) {
        sendFileListener = listener;
    }

    /**
     * 获取读取文件片段
     *
     * @param file  文件
     * @param start 开始位置
     * @return 文件片段通道
     * @throws Exception 文件异常
     */
    private FileChannel createClipFileChannel(File file, long start) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fileChannel = raf.getChannel();
        fileChannel = fileChannel.position(start);
        return fileChannel;
    }


    /**
     * 获取连接服务器通道
     *
     * @param host 地址
     * @param port 端口
     * @return socket通道
     * @throws IOException 读写异常
     */
    private SocketChannel createSocketChannel(String host, int port) throws IOException {
        SocketChannel sendChannel = SocketChannel.open();
        sendChannel.connect(new InetSocketAddress(host, port));
        return sendChannel;
    }


    class Sender implements Runnable {
        private SocketChannel sendChannel;
        private FileChannel fileChannel;
        private File file;
        private long start;
        private long partLength;
        private int partIndex;
        private volatile long sendSize;
        private boolean failed;//是否发送失败

        public long getSendSize() {
            return sendSize;
        }

        public boolean isFailed() {
            return failed;
        }

        public void setFailed() {
            this.failed = true;
        }

        public Sender(SocketChannel sendChannel,
                      FileChannel fileChannel,
                      File file,
                      long start,
                      long partLength,
                      int partIndex) {
            this.sendChannel = sendChannel;
            this.fileChannel = fileChannel;
            this.file = file;
            this.start = start;
            this.partLength = partLength;
            this.partIndex = partIndex;
        }

        @Override
        public void run() {
            sendFile(sendChannel, fileChannel, partLength, partIndex);
        }

        /**
         * 发送文件
         *
         * @param sendChannel 发送频道
         * @param fileChannel 文件频道
         */
        private void sendFile(SocketChannel sendChannel, FileChannel fileChannel, long partLength, int partIndex) {
            // 发送文件流
            try {
                //发送文件头信息
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(file.getName());
                fileInfo.setFileSize(file.length());
                fileInfo.setStart(start);
                fileInfo.setPartSize(partLength);
                fileInfo.setPartIndex(partIndex);
                fileInfo.setType(Constant.CONNECTION_TYPE_THREAD);
                sendChannel.write(ByteBuffer.wrap(fileInfo.toString().getBytes(Charset.forName("UTF-8"))));
                //等待应答信息(获取断点)
                ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
                int len;
                long breakPoint = 0;
                len = sendChannel.read(buffer);
                if (len != -1) {
                    buffer.flip();
                    //获取断点
                    breakPoint = buffer.getLong();
                }
                buffer.clear();
                sendSize += breakPoint;
                fileChannel = fileChannel.position(start + breakPoint);
                //开始发送文件
                while ((len = fileChannel.read(buffer)) != -1 && sendSize < partLength) {
                    buffer.flip();
                    if (sendSize + len > partLength) {
                        len = (int) (partLength - sendSize);
                        buffer.limit(len);
                    }
                    sendChannel.write(buffer);
                    buffer.clear();
                    sendSize += len;
                    if (failed) {
                        break;
                    }
                }
            } catch (Exception e) {
                failed = true;
                e.printStackTrace();
            } finally {
                try {
                    sendChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 发送文件
     *
     * @param file 文件
     * @param host 对方主机
     * @param port 对方端口
     */
    public static void send(File file, String host, int port, Map<String, List<Sender>> fileProgressMap) {
        if (file == null || !file.exists()) {
            return;
        }
        long length = file.length();
        if (length == 0) {
            return;
        }
        try {
            //1.创建文件发送对象
            FileSender fileSender = new FileSender();
            //2.发送文件信息等待应答
            SocketChannel askChannel = fileSender.createSocketChannel(host, port);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(file.getName());
            fileInfo.setFileSize(file.length());
            fileInfo.setType(Constant.CONNECTION_TYPE_REQUEST);
            askChannel.write(ByteBuffer.wrap(fileInfo.toString().getBytes(Charset.forName("UTF-8"))));
            //3.等待应答信息
            ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
            int len = 0;
            int code = 0;
            while ((len = askChannel.read(buffer)) != -1) {
                buffer.flip();
                byte[] array = buffer.array();
                code = array[0];
                if (len != buffer.capacity()) {
                    break;
                }
                buffer.clear();
            }
            //应答码不为0表示拒绝接收文件
            if (code != 0) {
                askChannel.close();
                if (sendFileListener != null) {
                    sendFileListener.onDenied(file.getName());
                }
                return;
            } else {
                if (sendFileListener != null) {
                    sendFileListener.onAccept(file.getName());
                }
            }
            //4.开始发送文件
            List<Sender> senderList = new ArrayList<>();
            int partIndex = 0;
            if (length > Constant.FILE_PART_SIZE) {
                //文件分段
                long part = (length + Constant.FILE_PART_NUM) / Constant.FILE_PART_NUM;
                long start = 0;
                do {
                    if (length - start < part) {
                        part = length - start;
                    }
                    Sender sender = fileSender.new Sender(
                            fileSender.createSocketChannel(host, port),
                            fileSender.createClipFileChannel(file, start),
                            file,
                            start,
                            part,
                            partIndex);
                    IOThreadPool.execute(sender);
                    senderList.add(sender);
                    partIndex++;
                } while ((start += part) < length);
            } else {
                //文件不分段
                Sender sender = fileSender.new Sender(
                        fileSender.createSocketChannel(host, port),
                        fileSender.createClipFileChannel(file, 0),
                        file,
                        0,
                        length,
                        partIndex);
                IOThreadPool.execute(sender);
                senderList.add(sender);
            }
            fileProgressMap.put(file.getName(), senderList);
        } catch (Exception e) {
            e.printStackTrace();
            if (sendFileListener != null) {
                sendFileListener.onSendFailed(file.getName());
            }
        }
    }

}
