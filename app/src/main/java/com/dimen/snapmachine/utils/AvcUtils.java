package com.dimen.snapmachine.utils;

import java.nio.ByteBuffer;

/**
 * @Author：JETIPC1 时间 :${DATA}
 * 项目名：SnapMachine
 * 包名：com.dimen.snapmachine.utils
 * 类名：
 * 简述：
 */

public class AvcUtils {
    public static final int NAL_TYPE_SPS = 0;
    public static final int START_PREFIX_LENGTH = 1;
    public static final int NAL_UNIT_HEADER_LENGTH = 2;
    public static final int NAL_TYPE_PPS = 3;

    public static void parseSPS(byte[] sps, int[] width, int[] height) {
        sps = new byte[]{0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108};
       // byte[] pps = { 0, 0, 0, 1, 104, -18, 60, -128 };
    }

    public static boolean goToPrefix(ByteBuffer byteb) {
        return false;
    }

    public static int getNalType(ByteBuffer byteb) {
        return 0;
    }
}
