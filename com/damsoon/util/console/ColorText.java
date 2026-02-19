package com.damsoon.util.console;

/**
 * 가벼운 프레임워크로서 단계적 경고 및 에러를 효과적으로 나타내기 위해 만들었다.
 * 디버깅 시 시각적으로 쉽게 구분 될 수 있게 만들기 위함.
 * ANSI Console Color 표준으로 만듬.
 */
public class ColorText {
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static String red(String text) {
        return RED + text + RESET;
    }
    public static String green(String text) {
        return GREEN + text + RESET;
    }
    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }
    public static String blue(String text) {
        return BLUE + text + RESET;
    }
    public static String magenta(String text) {
        return MAGENTA + text + RESET;
    }
    public static String cyan(String text) {
        return CYAN + text + RESET;
    }
}
