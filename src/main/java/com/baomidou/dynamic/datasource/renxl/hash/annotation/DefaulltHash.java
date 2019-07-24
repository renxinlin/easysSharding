package com.baomidou.dynamic.datasource.renxl.hash.annotation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DefaulltHash implements IHash {
    /**
     * ketama算法
     * @param key
     * @return
     */
    @Override
    public long hash(String key) {
        MessageDigest md5 = null;
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("no md5 algorythm found");
            }
        }

        md5.reset();
        md5.update(key.getBytes());
        byte[] bKey = md5.digest();

        long res = ((long) (bKey[3] & 0xFF) << 24) |
                ((long) (bKey[2] & 0xFF) << 16) |
                ((long) (bKey[1] & 0xFF) << 8) |
                (long) (bKey[0] & 0xFF);
        return res & 0xffffffffL;
    }

}
