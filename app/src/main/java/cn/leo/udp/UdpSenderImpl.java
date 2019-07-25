package cn.leo.udp;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author : Jarry Leo
 * @date : 2019/1/25 13:55
 */
class UdpSenderImpl implements UdpSender {
    private String remoteHost = "127.0.0.1";
    private int port = UdpConfig.DEFAULT_LISTEN_PORT;
    private PacketProcessor packetProcessor = new DefaultPacketProcessor();
    private UdpSendCore udpSendCore = new UdpSendCore();
    private List<String> broadcastHost = new ArrayList<>();

    UdpSenderImpl() {
    }

    UdpSenderImpl(String remoteHost, int port) {
        this.remoteHost = remoteHost;
        this.port = port;
    }

    UdpSenderImpl(String remoteHost, int port, PacketProcessor packetProcessor) {
        this.remoteHost = remoteHost;
        this.port = port;
        this.packetProcessor = packetProcessor;
    }

    @Override
    public UdpSender setRemoteHost(String host) {
        remoteHost = host;
        return this;
    }

    @Override
    public UdpSender setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public UdpSender setPacketProcessor(PacketProcessor packetProcessor) {
        this.packetProcessor = packetProcessor;
        return this;
    }

    @Override
    public UdpSender send(byte[] data) {
        List<byte[]> bytes = packetProcessor.subPacket(data);
        for (byte[] aByte : bytes) {
            udpSendCore.sendData(aByte, remoteHost, port);
        }
        return this;
    }

    @Override
    public UdpSender sendBroadcast(byte[] data) {
        if (broadcastHost.size() == 0) {
            getBroadcastHost();
        }
        if (broadcastHost.size() == 0) {
            System.out.println("broadcastHost not found !");
            return this;
        }
        List<byte[]> bytes = packetProcessor.subPacket(data);
        for (byte[] aByte : bytes) {
            for (String broadcast : broadcastHost) {
                udpSendCore.sendData(aByte, broadcast, port);
            }
        }
        return this;
    }

    private void getBroadcastHost() {
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                List<InterfaceAddress> iaList = ni.getInterfaceAddresses();
                for (InterfaceAddress ia : iaList) {
                    InetAddress iaAddress = ia.getAddress();
                    if (iaAddress instanceof Inet6Address) {
                        // skip ipv6
                        continue;
                    }
                    String ip = iaAddress.getHostAddress();
                    System.out.println(ip);
                    if (!"127.0.0.1".equals(ip)) {
                        broadcastHost.add(ia.getBroadcast().getHostAddress());
                        System.out.println("broadcastHost: " + broadcastHost);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
