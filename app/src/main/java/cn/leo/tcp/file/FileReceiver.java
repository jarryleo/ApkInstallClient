package cn.leo.tcp.file;

import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Jarry Leo
 * @date : 2019/1/26 9:32
 */
class FileReceiver extends Thread {
    private int port;
    private String dir;
    private boolean rename;
    private ReceiveFileListener receiveFileListener;
    private ConcurrentHashMap<String, List<Receiver>> receiverMap = new ConcurrentHashMap<>();

    public FileReceiver(int port, String dir, boolean rename, ReceiveFileListener receiveFileListener) {
        this.port = port;
        this.dir = dir;
        this.rename = rename;
        this.receiveFileListener = receiveFileListener;
    }

    public void setReceiveFileListener(ReceiveFileListener receiveFileListener) {
        this.receiveFileListener = receiveFileListener;
    }


    public ConcurrentHashMap<String, List<Receiver>> getReceiverMap() {
        return receiverMap;
    }

    @Override
    public void run() {
        receiveFile();
    }

    private void receiveFile() {
        try {
            createSocketChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createSocketChannel() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        for (; ; ) {
            SocketChannel receiveChannel = serverSocketChannel.accept();
            //判断连接是申请文件传输还是多线程文件传输
            dispatchChannel(receiveChannel);
        }
    }

    //区分连接类型
    private void dispatchChannel(SocketChannel receiveChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int readLength = 0;
        while ((readLength = receiveChannel.read(buffer)) > 0) {
            buffer.flip();
            baos.write(buffer.array());
            buffer.clear();
            // 最后一包读取特殊处理,不然会一直等待读入
            if (readLength != buffer.capacity()) {
                break;
            }
        }
        String json = baos.toString("UTF-8");
        FileInfo fileInfo = JSONObject.parseObject(json, FileInfo.class);
        if (fileInfo.getType() == Constant.CONNECTION_TYPE_REQUEST) {
            //文件请求类型
            isRequest(receiveChannel, fileInfo);
        } else if (fileInfo.getType() == Constant.CONNECTION_TYPE_THREAD) {
            //文件传输类型
            fileReceive(receiveChannel, fileInfo);
        }

    }

    private void fileReceive(SocketChannel receiveChannel, FileInfo fileInfo) {
        try {
            String fileName = fileInfo.getFileName();
            long start = fileInfo.getStart();
            //创建临时文件接收，接收完成之后再重命名
            File file = new File(dir, fileName + ".tmp");
            File breakPointFile = new File(dir, fileName + ".bp");
            int partIndex = fileInfo.getPartIndex();
            long breakPoint = 0;
            if (breakPointFile.exists()) {
                //如果文件存在就读取保存的断点记录
                breakPoint = BreakPoint.getPoint(breakPointFile, partIndex);
            }
            FileChannel fileChannel = createClipFileChannel(file, start);
            Receiver receiver = new Receiver(receiveChannel, fileChannel, fileInfo, breakPoint);
            IOThreadPool.execute(receiver);
            List<Receiver> receiverList = receiverMap.get(fileName);
            if (receiverList == null) {
                receiverList = new ArrayList<>();
                receiverMap.put(fileName, receiverList);
            }
            receiverList.add(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void isRequest(final SocketChannel receiveChannel, FileInfo fileInfo) throws IOException {
        if (receiveFileListener == null) {
            //没有监听，自动同意接收文件
            sendAcceptCode(receiveChannel);
        } else {
            receiveFileListener.onNewFile(fileInfo, new NewFileRequest() {
                @Override
                public void accept() {
                    //同意接收文件
                    try {
                        sendAcceptCode(receiveChannel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void denied() {
                    //拒绝接受文件
                    try {
                        sendDeniedCode(receiveChannel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void sendAcceptCode(SocketChannel receiveChannel) throws IOException {
        //发送同意接收文件的代码
        byte[] array = new byte[1];
        receiveChannel.write(ByteBuffer.wrap(array));
        receiveChannel.close();
    }

    private void sendDeniedCode(SocketChannel receiveChannel) throws IOException {
        //发送拒绝接受文件的代码
        byte[] array = new byte[1];
        array[0] = 1;
        receiveChannel.write(ByteBuffer.wrap(array));
        receiveChannel.close();
    }

    private FileChannel createClipFileChannel(File file, long start) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = raf.getChannel();
        fileChannel = fileChannel.position(start);
        return fileChannel;
    }


    class Receiver implements Runnable {
        private SocketChannel receiveChannel;
        private FileChannel fileChannel;
        private FileInfo fileInfo;
        private long start;
        private long partSize;
        private long breakPoint;
        private volatile long size;
        private boolean failed;//是否接收失败

        public Receiver(SocketChannel receiveChannel, FileChannel fileChannel, FileInfo fileInfo, long breakPoint) {
            this.receiveChannel = receiveChannel;
            this.fileChannel = fileChannel;
            this.fileInfo = fileInfo;
            this.start = fileInfo.getStart();
            this.partSize = fileInfo.getPartSize();
            this.breakPoint = breakPoint;
        }

        public boolean isFailed() {
            return failed;
        }

        public void setFailed() {
            this.failed = true;
        }

        public long getStart() {
            return start;
        }

        public long getPartSize() {
            return partSize;
        }

        public long getReceivedSize() {
            return size;
        }

        public FileInfo getFileInfo() {
            return fileInfo;
        }

        @Override
        public void run() {
            receiveFile(receiveChannel, fileChannel);
        }

        private void receiveFile(SocketChannel receiveChannel, FileChannel fileChannel) {
            try {
                //读取断点告诉对方从哪里开始发送
                ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
                buffer.putLong(breakPoint);
                buffer.flip();
                receiveChannel.write(buffer);
                buffer.clear();
                fileChannel = fileChannel.position(start + breakPoint);
                size += breakPoint;
                //开始接收文件
                int len = 0;
                while ((len = receiveChannel.read(buffer)) >= 0) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    buffer.clear();
                    size += len;
                    if (failed) {
                        break;
                    }
                }
            } catch (Exception e) {
                failed = true;
                e.printStackTrace();
            } finally {
                try {
                    receiveChannel.close();
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

}
