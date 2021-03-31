package top.zibin.luban.example.quality;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * ————————————————
 *     版权声明：本文为CSDN博主「番茄大圣」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
 *     原文链接：https://blog.csdn.net/tomatomas/article/details/62235963
 */
public class QuantTables {
    static final int MAX_TABLE_COUNT = 2;
    static final int TABLE_LENGTH = 64;

    private int tables[][] = new int[MAX_TABLE_COUNT][TABLE_LENGTH];
    private byte tablesInitFlag[] = new byte[MAX_TABLE_COUNT];

    private static final byte JPEG_FLAG_START = (byte) 0xff;
    private static final byte JPEG_FLAG_DQT = (byte) 0xdb;// define quantization  table

    private static final byte[] JPEG_HEADER_FLAG = new byte[] { (byte) 0xff, (byte) 0xd8 };

    void getDataFromFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buf = new byte[6];
            int len = -1;

            len = fis.read(buf, 0, 6);

            if (len < 6)
                return;

            if (buf[0] != JPEG_HEADER_FLAG[0] || buf[1] != JPEG_HEADER_FLAG[1])// it's not a jpeg file so return
                return;

            int index = 2;

            while (len > index) {
                int sectionLength = byteToInt(buf[index + 2]) * 0x100 + byteToInt(buf[index + 3]);

                if (buf[index] != JPEG_FLAG_START) {
                    break;
                }

                if (buf[index + 1] == JPEG_FLAG_DQT) {// it's a begin of DQT
                    buf = new byte[sectionLength - 2];// dqt 长度不超过4个表长，即不超过4*64再加一些标志位
                    len = fis.read(buf, 0, buf.length);

                    if (len < buf.length)
                        break;// file is not complete
                    index = 0;

                    while (index < len) {
                        byte flag = buf[index];
                        byte high_precision = (byte) (flag >> 4);
                        byte low_id = (byte) (flag & 0x0f);

                        if (high_precision != 0) {
                            // don't know how to deal with high precision table
                            return;
                        }

                        if (low_id < 0 || low_id > 1)
                            return;

                        if (tablesInitFlag[low_id] != 0) {
                            // table already got,don't know how to deal with this,just clear and return
                            tablesInitFlag[0] = 0;
                            tablesInitFlag[1] = 0;
                            return;
                        }

                        tablesInitFlag[low_id] = 1;
                        for (int i = 0; i < tables[low_id].length; i++) {
                            tables[low_id][i] = buf[index + 1 + i];
                        }

                        index += 65;
                    }
                } else {
                    fis.skip(sectionLength - 2);
                    len = fis.read(buf, 0, 4);
                    index = 0;
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(fis);
        }
    }

    private int byteToInt(byte b) {
        return b & 0xff;
    }

    int[] getTable(int index) {
        if (index >= MAX_TABLE_COUNT || index < 0)
            return null;
        if (tablesInitFlag[index] == 0)
            return null;
        return tables[index];
    }

    boolean hasData() {
        for (int i = 0; i < MAX_TABLE_COUNT; i++) {
            if (tablesInitFlag[i] != 0)
                return true;
        }
        return false;
    }

    private void closeStream(FileInputStream fis) {
        if (fis != null)
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

}
