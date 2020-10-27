package com.allen.tinypng

/**
 * Create On 27/10/2020
 * @author liyong1
 */
class TinyPngExtension {
    ArrayList<String> apiKeys
    ArrayList<String> whiteList
    ArrayList<String> resourceDir
    ArrayList<String> resourcePattern
    boolean enable

    TinyPngExtension() {
        apiKeys = []
        whiteList = []
        resourceDir = []
        resourcePattern = []
        enable = true
    }

    @Override
    String toString() {
        return "TinyPngExtension{" +
                "apiKey='" + apiKeys + '\'' +
                ", whiteList=" + whiteList +
                ", resourceDir=" + resourceDir +
                ", enable=" + enable +
                ", resourcePattern=" + resourcePattern +
                '}';
    }
}
