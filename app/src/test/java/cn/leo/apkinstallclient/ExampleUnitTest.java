package cn.leo.apkinstallclient;

import org.junit.Test;

import java.nio.charset.Charset;

import cn.leo.udp.OnDataArrivedListener;
import cn.leo.udp.UdpFrame;
import cn.leo.udp.UdpSender;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        UdpFrame.getListener().subscribe(25678, new OnDataArrivedListener() {
            @Override
            public void onDataArrived(byte[] data, String host, int port) {
                System.out.println(host + ":");
                System.out.println(new String(data,Charset.defaultCharset()));
            }
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UdpSender sender = UdpFrame.getSender(25678);
        sender.sendBroadcast("test broadcast".getBytes(Charset.defaultCharset()));
    }
}