package io.github.cesarsdevs.minebot;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class PacketUtils {
	
    public static byte[] readByteArray(final DataInputStream dataInputStream) {
        final int length = readVarInt(dataInputStream);
        final byte[] data = new byte[length];
        
        try {
            dataInputStream.readFully(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return data;
    }

    public static int readVarInt(final DataInputStream dataInputStream) {
        int numRead = 0;
        int result = 0;
        
        try {
            byte read;
            do {
                read = dataInputStream.readByte();

                final int value = read & 127;
                result |= value << 7 * numRead;
                
                if (++numRead <= 5) {
                	continue;
                }
                
                throw new RuntimeException("VarInt is too big");
            } while ((read & 128) != 0);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        return result;
    }

    public static void writeVarInt(final DataOutputStream out, int paramInt) {
        try {
            do {
                if ((paramInt & -128) == 0) {
                    out.writeByte(paramInt);
                    return;
                }
                
                out.writeByte(paramInt & 127 | 128);
                paramInt >>>= 7;
            } while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeByteArray(final DataOutputStream out, final byte[] data) {
        try {
            writeVarInt(out, data.length);
            out.write(data, 0, data.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writePacket(final DataOutputStream out, final byte[] packet) {
        try {
            writeVarInt(out, packet.length);
            out.write(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createEncryptionResponsePacket(final byte[] encryptedKey, final byte[] encryptedVerifyToken) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
       
        writeVarInt(dataOutputStream, 1);
        writeByteArray(dataOutputStream, encryptedKey);
        writeByteArray(dataOutputStream, encryptedVerifyToken);

        final byte[] data = byteArrayOutputStream.toByteArray();
        
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return data;
    }
    
    public static byte[] createHandshakeMessage18(final String host, final int port, final int state) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(0);
            
            writeVarInt(dataOutputStream, 47);
            writeString(dataOutputStream, host, StandardCharsets.UTF_8);
            dataOutputStream.writeShort(port);
            writeVarInt(dataOutputStream, state);
            
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createLogin(final String username) {
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final DataOutputStream login = new DataOutputStream(buffer);
            login.writeByte(0);
            
            writeString(login, username, StandardCharsets.UTF_8);
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeString(final DataOutputStream dataOutputStream, final String string, final Charset charset) {
        try {
            final byte[] bytes = string.getBytes(charset);
            writeVarInt(dataOutputStream, bytes.length);
            dataOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendPacket(final byte[] packet, final DataOutputStream dataOutputStream) {
        writePacket(dataOutputStream, packet);
    }

}

