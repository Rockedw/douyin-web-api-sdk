package com.dy_web_api.sdk.message.utils;


import org.bouncycastle.crypto.digests.SM3Digest;
import java.util.Random;

public class ABogusUtil {
    private final static int AID = 6383;
    private final static int PAGE_ID = 0;
    private final static String SALT = "cus";
    private final static byte[] UA_KEY = new byte[]{0, 1, 14};
    private final static char[] BASE64_ALPHABET_0 = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe".toCharArray();
    private final static char[] BASE64_ALPHABET_1 = "ckdp1h4ZKsUB80/Mfvw36XIgR25+WQAlEi7NLboqYTOPuzmFjJnryx9HVGDaStCe".toCharArray();
    private final static int[] SORT_INDEX_ARRAY_1 = new int[]{18, 20, 52, 26, 30, 34, 58, 38, 40, 53, 42, 21, 27, 54, 55, 31, 35, 57, 39, 41, 43, 22, 28, 32, 60, 36, 23, 29, 33, 37, 44, 45, 59, 46, 47, 48, 49, 50, 24, 25, 65, 66, 70, 71};
    private final static int[] SORT_INDEX_ARRAY_2 = new int[]{18, 20, 26, 30, 34, 38, 40, 42, 21, 27, 31, 35, 39, 41, 43, 22, 28, 32, 36, 23, 29, 33, 37, 44, 45, 46, 47, 48, 49, 50, 24, 25, 52, 53, 54, 55, 57, 58, 59, 60, 65, 66, 70, 71};
    private final static int BASE64_PADDING_MODE_COMMON = 0;
    private final static int BASE64_PADDING_MODE_A_BOGUS = 1;

    static String randomFingerprint() {
        Random random = new Random();
        int innerWidth = random.nextInt(1024, 1920);
        int innerHeight = random.nextInt(768, 1080);
        int outerWidth = innerWidth + random.nextInt(24, 32);
        int outerHeight = innerHeight + random.nextInt(75, 90);
        int screenX = 0;
        int screenY = new int[]{0, 30}[random.nextInt(2)];
        int availWidth = random.nextInt(1280, 1920);
        int availHeight = random.nextInt(800, 1080);
        int[] data = new int[]{
                innerWidth,
                innerHeight,
                outerWidth,
                outerHeight,
                screenX,
                screenY,
                0,
                0,
                availWidth,
                availHeight,
                availWidth,
                availHeight,
                innerWidth,
                innerHeight,
                24,
                24
        };
        StringBuilder builder = new StringBuilder();
        for(int d : data) {
            builder.append(d).append("|");
        }
        builder.append("|").append("win");
        return builder.toString();
    }

    static String addSalt(String content) {
        return content + SALT;
    }

    static byte[] sm3(byte[] in) {
        SM3Digest digest = new SM3Digest();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    static byte[] rc4(String in) {
        int[] s = new int[256];
        for(int i = 0; i < 256; i++) {
            s[i] = i;
        }
        int i, j = 0;
        for(i = 0; i < 256; i++) {
            j = (j + s[i] + UA_KEY[i % UA_KEY.length]) % 256;
            int temp = s[i];
            s[i] = s[j];
            s[j] = temp;
        }
        char[] chars = in.toCharArray();
        i = j = 0;
        byte[] out = new byte[chars.length];
        int index = 0;
        for(char c : chars) {
            i = (i + 1) % 256;
            j = (j + s[i]) % 256;
            int temp = s[i];
            s[i] = s[j];
            s[j] = temp;
            int k = s[(s[i] + s[j]) % 256];
            out[index++] = (byte) (c ^ k);
        }
        return out;
    }

    static byte[] transform(int[] in) {
        int[] big = new int[]{121, 243, 55, 234, 103, 36, 47, 228, 30, 231, 106, 6, 115, 95, 78, 101, 250, 207, 198, 50, 139, 227, 220, 105, 97, 143, 34, 28, 194, 215, 18, 100, 159, 160, 43, 8, 169, 217, 180, 120, 247, 45, 90, 11, 27, 197, 46, 3, 84, 72, 5, 68, 62, 56, 221, 75, 144, 79, 73, 161, 178, 81, 64, 187, 134, 117, 186, 118, 16, 241, 130, 71, 89, 147, 122, 129, 65, 40, 88, 150, 110, 219, 199, 255, 181, 254, 48, 4, 195, 248, 208, 32, 116, 167, 69, 201, 17, 124, 125, 104, 96, 83, 80, 127, 236, 108, 154, 126, 204, 15, 20, 135, 112, 158, 13, 1, 188, 164, 210, 237, 222, 98, 212, 77, 253, 42, 170, 202, 26, 22, 29, 182, 251, 10, 173, 152, 58, 138, 54, 141, 185, 33, 157, 31, 252, 132, 233, 235, 102, 196, 191, 223, 240, 148, 39, 123, 92, 82, 128, 109, 57, 24, 38, 113, 209, 245, 2, 119, 153, 229, 189, 214, 230, 174, 232, 63, 52, 205, 86, 140, 66, 175, 111, 171, 246, 133, 238, 193, 99, 60, 74, 91, 225, 51, 76, 37, 145, 211, 166, 151, 213, 206, 0, 200, 244, 176, 218, 44, 184, 172, 49, 216, 93, 168, 53, 21, 183, 41, 67, 85, 224, 155, 226, 242, 87, 177, 146, 70, 190, 12, 162, 19, 137, 114, 25, 165, 163, 192, 23, 59, 9, 94, 179, 107, 35, 7, 142, 131, 239, 203, 149, 136, 61, 249, 14, 156};
        byte[] out = new byte[in.length];
        int j = big[1];
        int initial = 0;
        int sum;
        int e = 0;
        for(int i = 0; i < in.length; i++) {
            int b = in[i];
            if(i == 0) {
                initial = big[j];
                sum = j + initial;
                big[1] = initial;
                big[j] = j;
            } else {
                sum = initial + e;
            }
            sum %= big.length;
            int f = big[sum];
            out[i] = (byte) (b ^ f);
            e = big[i + 2] % big.length;
            sum = (j + e) % big.length;
            initial = big[sum];
            int k = (i + 2) % big.length;
            big[sum] = big[k];
            big[k] = initial;
            j = sum;
        }
        return out;
    }

    static String base64(byte[] in, char[] alphabet, int paddingMode) {
        if(in == null || in.length == 0) return null;

        StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            int n = Byte.toUnsignedInt(b);
            String binary = Integer.toBinaryString(n);
            if(binary.length() < 8) builder.append("0".repeat(8 - binary.length()));
            builder.append(binary);
        }
        builder.append("0".repeat(Math.max(0, (6 - in.length * 8 % 6) % 6)));
        String binary = builder.toString();

        StringBuilder result = new StringBuilder();
        for(int i = 0; i < binary.length(); i = i + 6) {
            String segment = binary.substring(i, i + 6);
            int index = Integer.parseInt(segment, 2);
            char c = alphabet[index];
            result.append(c);
        }

        if(paddingMode == 0) {
            int paddingLength = ((6 - in.length * 8 % 6) % 6 + 1) / 2;
            result.append("=".repeat(paddingLength));
        } else if(paddingMode == 1) {
            int paddingLength = (4 - result.length() % 4) % 4;
            result.append("=".repeat(paddingLength));
        }
        return result.toString();
    }


    public static String generateABogus(String params, String userAgent) {
        String fingerprint = randomFingerprint();

        long startEncryption = System.currentTimeMillis() / 1000 * 1000;
        byte[] bytes1 = sm3(sm3(addSalt(params).getBytes()));
        byte[] bytes2 = sm3(sm3(addSalt("GET").getBytes()));
        byte[] bytes3 = sm3(base64(rc4(userAgent), BASE64_ALPHABET_1, BASE64_PADDING_MODE_COMMON).getBytes());
        long endEncryption = System.currentTimeMillis() / 1000 * 1000;

        int[] data = new int[128];
        data[8] = 3;
        data[18] = 44;

        data[20] = (int) ((startEncryption >> 24) & 255);
        data[21] = (int) ((startEncryption >> 16) & 255);
        data[22] = (int) ((startEncryption >> 8) & 255);
        data[23] = (int) (startEncryption & 255);
        data[24] = (int) (startEncryption / 256 / 256 / 256 / 256);
        data[25] = (int) (startEncryption / 256 / 256 / 256 / 256 / 256);

        data[31] = 1 % 256 & 255;
        data[37] = 14 & 255;

        data[38] = Byte.toUnsignedInt(bytes1[21]);
        data[39] = Byte.toUnsignedInt(bytes1[22]);
        data[40] = Byte.toUnsignedInt(bytes2[21]);
        data[41] = Byte.toUnsignedInt(bytes2[22]);
        data[42] = Byte.toUnsignedInt(bytes3[23]);
        data[43] = Byte.toUnsignedInt(bytes3[24]);

        data[44] = (int) ((endEncryption >> 24) & 255);
        data[45] = (int) ((endEncryption >> 16) & 255);
        data[46] = (int) ((endEncryption >> 8) & 255);
        data[47] = (int) (endEncryption & 255);
        data[48] = data[8];
        data[49] = (int) (endEncryption / 256 / 256 / 256 / 256);
        data[50] = (int) (endEncryption / 256 / 256 / 256 / 256 / 256);

        data[51] = (PAGE_ID >> 24) & 255;
        data[52] = (PAGE_ID >> 16) & 255;
        data[53] = (PAGE_ID >> 8) & 255;
        data[54] = PAGE_ID & 255;
        data[55] = PAGE_ID;
        data[56] = AID;
        data[57] = AID & 255;
        data[58] = (AID >> 8) & 255;
        data[59] = (AID >> 16) & 255;
        data[60] = (AID >> 24) & 255;

        data[64] = fingerprint.length();
        data[65] = fingerprint.length();

        int[] sortValueData = new int[SORT_INDEX_ARRAY_1.length];
        for(int i = 0; i < SORT_INDEX_ARRAY_1.length; i++) {
            sortValueData[i] = data[SORT_INDEX_ARRAY_1[i]];
        }

        int[] fingerprintData = new int[fingerprint.length()];
        for(int i = 0; i < fingerprint.toCharArray().length; i++) {
            char c = fingerprint.charAt(i);
            fingerprintData[i] = c;
        }

        int xor = 0;
        for(int i = 0; i < SORT_INDEX_ARRAY_2.length - 1; i++) {
            if(i == 0) {
                xor = data[SORT_INDEX_ARRAY_2[i]];
            }
            xor ^= data[SORT_INDEX_ARRAY_2[i + 1]];
        }

        int[] valueData = new int[sortValueData.length + fingerprintData.length + 1];
        System.arraycopy(sortValueData, 0, valueData, 0, sortValueData.length);
        System.arraycopy(fingerprintData, 0, valueData, sortValueData.length, fingerprintData.length);
        valueData[valueData.length - 1] = xor;

        byte[] randomData = new byte[12];
        for(int i = 0; i < 3; i++) {
            int rd = new Random().nextInt(10000);
            randomData[i * 4] = (byte) (((rd & 255) & 170) | 1);
            randomData[i * 4 + 1] = (byte) (((rd & 255) & 85) | 2);
            randomData[i * 4 + 2] = (byte) (((rd >> 8) & 170) | 5);
            randomData[i * 4 + 3] = (byte) (((rd >> 8) & 85) | 40);
        }

        byte[] transformData = transform(valueData);
        byte[] finalData = new byte[randomData.length + transformData.length];
        System.arraycopy(randomData, 0, finalData, 0, randomData.length);
        System.arraycopy(transformData, 0, finalData, randomData.length, transformData.length);

        return base64(finalData, BASE64_ALPHABET_0, BASE64_PADDING_MODE_A_BOGUS);
    }
}