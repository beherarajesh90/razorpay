package com.systemdesign.razorpay.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String randomBase64(int length){
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        // why url encoder? replace +, / with url safe chars - and _ which are suitable for jwt, api keys etc
        // without padding means base64 produces BAxk9A==. = or == is removed
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
