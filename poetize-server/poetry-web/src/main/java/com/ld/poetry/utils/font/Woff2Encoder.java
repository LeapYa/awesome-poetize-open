package com.ld.poetry.utils.font;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;

/**
 * WOFF2 编码器 — 将 TTF 字节数组编码为 WOFF2 格式
 * <p>
 * WOFF2 规范: <a href="https://www.w3.org/TR/WOFF2/">W3C WOFF2</a>
 * <p>
 * 简化实现: 使用 "单一压缩块" 策略把整个 TTF 做 Brotli 压缩,
 * 不做 table transform (transformLength 全为 0). 浏览器兼容性良好.
 */
@Slf4j
public class Woff2Encoder {

    /** WOFF2 签名: 'wOF2' */
    private static final int WOFF2_SIGNATURE = 0x774F4632;

    /** 已知的 SFNT 表标签 (按 WOFF2 规范顺序, 用于 table flag 编码) */
    private static final String[] KNOWN_TAGS = {
            "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post",
            "cvt ", "fpgm", "glyf", "loca", "prep", "CFF ", "VORG", "EBDT",
            "EBLC", "gasp", "hdmx", "kern", "LTSH", "PCLT", "VDMX", "vhea",
            "vmtx", "BASE", "GDEF", "GPOS", "GSUB", "EBSC", "JSTF", "MATH",
            "CBDT", "CBLC", "COLR", "CPAL", "SVG ", "sbix", "acnt", "avar",
            "bdat", "bloc", "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar",
            "gvar", "hsty", "just", "lcar", "mort", "morx", "opbd", "prop",
            "trak", "Zapf", "Silf", "Glat", "Gloc", "Feat", "Sill"
    };

    static {
        // 确保 Brotli4j native 库已加载
        Brotli4jLoader.ensureAvailability();
    }

    /**
     * 将 TTF 字节编码为 WOFF2
     *
     * @param ttfBytes 原始 TTF 文件字节
     * @return WOFF2 文件字节
     */
    public static byte[] encode(byte[] ttfBytes) throws IOException {
        ByteBuffer ttf = ByteBuffer.wrap(ttfBytes).order(ByteOrder.BIG_ENDIAN);

        // ---------- 1. 解析 TTF offset table ----------
        int sfntVersion = ttf.getInt(0); // 0x00010000 (TrueType) 或 'OTTO' (CFF)
        int numTables = ttf.getShort(4) & 0xFFFF;

        // ---------- 2. 读取每张 table 的 record ----------
        int[][] tableRecords = new int[numTables][4]; // tag, checksum, offset, length
        for (int i = 0; i < numTables; i++) {
            int recordOffset = 12 + i * 16;
            tableRecords[i][0] = ttf.getInt(recordOffset); // tag
            tableRecords[i][1] = ttf.getInt(recordOffset + 4); // checksum
            tableRecords[i][2] = ttf.getInt(recordOffset + 8); // offset
            tableRecords[i][3] = ttf.getInt(recordOffset + 12); // length
        }

        // ---------- 3. 将所有 table 数据连续写入 (按原始文件顺序, 4字节对齐), 然后 Brotli 压缩 ----------
        ByteArrayOutputStream rawTablesStream = new ByteArrayOutputStream();
        int totalOrigLength = 0;
        int[] transformedLengths = new int[numTables]; // 全为原始长度 (不做 transform)
        for (int i = 0; i < numTables; i++) {
            int off = tableRecords[i][2];
            int len = tableRecords[i][3];
            rawTablesStream.write(ttfBytes, off, len);
            transformedLengths[i] = len;
            totalOrigLength += len;
            // 4 字节对齐 padding
            int padding = (4 - (len % 4)) % 4;
            for (int p = 0; p < padding; p++) {
                rawTablesStream.write(0);
            }
            totalOrigLength += padding;
        }
        byte[] rawTables = rawTablesStream.toByteArray();

        // Brotli 压缩
        byte[] compressedTables = brotliCompress(rawTables);

        // ---------- 4. 构建 WOFF2 table directory (变长编码) ----------
        ByteArrayOutputStream tableDir = new ByteArrayOutputStream();
        for (int i = 0; i < numTables; i++) {
            int tag = tableRecords[i][0];
            int origLength = tableRecords[i][3];

            // flags byte: bits 0-5 = known tag index (63 = arbitrary tag)
            int knownIndex = findKnownTagIndex(tag);
            int flags = (knownIndex & 0x3F); // bits 6-7: transformVersion = 0 (no transform)
            tableDir.write(flags);

            // 若 knownIndex == 63, 写完整 4 字节 tag
            if (knownIndex == 63) {
                write32(tableDir, tag);
            }

            // origLength (UIntBase128)
            writeUIntBase128(tableDir, origLength);
            // transformLength — 因为 transformVersion=0 且不是 glyf/loca, 省略
            // (对 glyf/loca 在 transformVersion=0 时也省略)
        }
        byte[] tableDirBytes = tableDir.toByteArray();

        // ---------- 5. 组装 WOFF2 文件 ----------
        int headerSize = 48; // WOFF2 fixed header = 48 bytes
        int totalSize = headerSize + tableDirBytes.length + compressedTables.length;

        ByteBuffer woff2 = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN);

        // WOFF2 Header (48 bytes)
        woff2.putInt(WOFF2_SIGNATURE); // signature
        woff2.putInt(sfntVersion); // flavor
        woff2.putInt(totalSize); // length
        woff2.putShort((short) numTables); // numTables
        woff2.putShort((short) 0); // reserved
        woff2.putInt(ttfBytes.length); // totalSfntSize (原始TTF大小)
        woff2.putInt(compressedTables.length); // totalCompressedSize
        woff2.putShort((short) 1); // majorVersion
        woff2.putShort((short) 0); // minorVersion
        woff2.putInt(0); // metaOffset
        woff2.putInt(0); // metaLength
        woff2.putInt(0); // metaOrigLength
        woff2.putInt(0); // privOffset
        woff2.putInt(0); // privLength

        // Table directory
        woff2.put(tableDirBytes);

        // Compressed data
        woff2.put(compressedTables);

        return woff2.array();
    }

    /** 在已知表标签列表中查找, 找不到返回 63 */
    private static int findKnownTagIndex(int tag) {
        String tagStr = tagToString(tag);
        for (int i = 0; i < KNOWN_TAGS.length; i++) {
            if (KNOWN_TAGS[i].equals(tagStr)) {
                return i;
            }
        }
        return 63;
    }

    private static String tagToString(int tag) {
        byte[] b = new byte[4];
        b[0] = (byte) (tag >> 24);
        b[1] = (byte) (tag >> 16);
        b[2] = (byte) (tag >> 8);
        b[3] = (byte) tag;
        return new String(b, StandardCharsets.US_ASCII);
    }

    /** UIntBase128 变长编码 (WOFF2 规范) */
    private static void writeUIntBase128(OutputStream out, int value) throws IOException {
        // 最多 5 字节
        int[] bytes = new int[5];
        int count = 0;
        do {
            bytes[count++] = value & 0x7F;
            value >>= 7;
        } while (value > 0);

        // 反序写出, 高位字节设 bit7=1 (非最后字节)
        for (int i = count - 1; i >= 0; i--) {
            int b = bytes[i];
            if (i > 0) {
                b |= 0x80;
            }
            out.write(b);
        }
    }

    private static void write32(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    /** Brotli 压缩 */
    private static byte[] brotliCompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Encoder.Parameters params = new Encoder.Parameters().setQuality(11); // 最大压缩
        try (BrotliOutputStream bos = new BrotliOutputStream(baos, params)) {
            bos.write(data);
        }
        return baos.toByteArray();
    }
}
