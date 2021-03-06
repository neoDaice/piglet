
package com.dc.piglet.rtmp.core.io.f4v.box;


import com.dc.piglet.rtmp.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;

public class MP4Descriptor {
    private static final Logger log = LoggerFactory.getLogger(MP4Descriptor.class);
    public final static int ES_TAG = 3;
    public final static int DECODER_CONFIG = 4;
    public final static int DECODER_SPECIFIC_CONFIG = 5;
    private byte[] decoderSpecificConfig = Util.fromHex("af0013100000");

    public MP4Descriptor(ByteBuf in) {
        final int size = in.readInt();
        in.readInt(); // TODO check that this is in-fact "esds"
        in.readInt(); // version and flags
        while (in.isReadable()) {
            readDescriptor(in, size - 12);
        }
    }

    public byte[] getConfigBytes() {
        return decoderSpecificConfig;
    }

    private int readDescriptor(ByteBuf bitstream, int length) {
        final int tag = bitstream.readByte();
        int size = 0;
        int b = 0;
        int read = 1;
        do {
            b = bitstream.readByte();
            size <<= 7;
            size |= b & 0x7f;
            read++;
        } while ((b & 0x80) == 0x80);
        switch (tag) {
            case ES_TAG:
                return parseES(bitstream, length - read) + read;
            case DECODER_CONFIG:
                return parseDecoderConfig(bitstream, length - read) + read;
            case DECODER_SPECIFIC_CONFIG:
                return parseDecoderSpecificConfig(bitstream, size, length - read) + read;
            default:
                bitstream.skipBytes(size);
                return size + read;
        }
    }

    private int parseES(ByteBuf bitstream, int length) {
        int read = 3;
        int ES_ID = bitstream.readShort();
        int flags = bitstream.readByte();
        boolean streamDependenceFlag = (flags & (1 << 7)) != 0;
        boolean urlFlag = (flags & (1 << 6)) != 0;
        boolean ocrFlag = (flags & (1 << 5)) != 0;
        if (streamDependenceFlag) {
            bitstream.skipBytes(2);
            read += 2;
        }
        if (urlFlag) {
            int str_size = bitstream.readByte();
            bitstream.skipBytes(str_size);
            read += str_size;
        }
        if (ocrFlag) {
            bitstream.skipBytes(2);
            read += 2;
        }
        while (bitstream.readableBytes() > length - read) {
            read += readDescriptor(bitstream, length - read);
        }
        return read;
    }

    private int parseDecoderConfig(ByteBuf bitstream, int length) {
        final int objectTypeIndication = bitstream.readByte();
        int value = bitstream.readByte();
        final boolean upstream = (value & (1 << 1)) > 0;
        final byte streamType = (byte) (value >> 2);
        value = bitstream.readShort();
        int bufferSizeDB = value << 8;
        value = bitstream.readByte();
        bufferSizeDB |= value & 0xff;
        final int maxBitRate = bitstream.readInt();
        final int minBitRate = bitstream.readInt();
        int read = 13;
        while (bitstream.readableBytes() > length - 13) {
            read += readDescriptor(bitstream, length - 13);
        }
        return read;
    }

    private int parseDecoderSpecificConfig(ByteBuf bitstream, int size, int length) {
        decoderSpecificConfig = new byte[size];
        bitstream.readBytes(decoderSpecificConfig);
        return size;
    }

}
