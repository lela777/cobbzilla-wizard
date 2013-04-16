package org.cobbzilla.wizardtest;

import org.apache.commons.lang.RandomStringUtils;

public class RandomUtil {

    public static final String TEST_EMAIL_SUFFIX = "@example.com";

    public static String randomName() { return randomName(20); }

    public static String randomName(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public static String randomEmail () {
        return randomName(5) + System.currentTimeMillis() + TEST_EMAIL_SUFFIX;
    }
    public static String randomEmail (int length) {
        final String baseEmail = System.currentTimeMillis() + TEST_EMAIL_SUFFIX;
        final int emailLength = Math.max(10, length - baseEmail.length());
        return randomName(emailLength) + baseEmail;
    }

}
