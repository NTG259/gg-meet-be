package com.meet.BE.utils;

import java.util.Random;

public class GenerateRoomCode {
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new Random();

    public static String generateCode() {
        return randomPair() + "-" + randomPair() + "-" + randomPair();
    }

    private static String randomPair() {
        return "" + CHARS.charAt(RANDOM.nextInt(CHARS.length()))
                + CHARS.charAt(RANDOM.nextInt(CHARS.length()));
    }

    public static void main(String[] args) {
        System.out.println(generateCode());
    }
}
