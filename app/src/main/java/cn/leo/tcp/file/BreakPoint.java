package cn.leo.tcp.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author : Jarry Leo
 * @date : 2019/2/11 10:45
 * <p>
 * 断点记录和读取
 */
public class BreakPoint {
    public static void savePoint(File file, int index, long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        FileChannel fileChannel = null;
        try {
            fileChannel = createClipFileChannel(file, index * 8);
            buffer.clear();
            buffer.putLong(value);
            buffer.flip();
            fileChannel.write(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long getPoint(File file, int index) {
        long value = 0;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        FileChannel fileChannel = null;
        try {
            fileChannel = createClipFileChannel(file, index * 8);
            int read = fileChannel.read(buffer);
            if (read != -1) {
                buffer.flip();
                value = buffer.getLong();
                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    private static FileChannel createClipFileChannel(File file, long start) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = raf.getChannel();
        fileChannel = fileChannel.position(start);
        return fileChannel;
    }
}
