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
        apiKeys = ['6TgBR0Tx6G83P98kMVF1SvPbsFjfl0qk']
        whiteList = []
        resourceDir = []
        resourcePattern = []
        enable = true
    }

    @Override
    String toString() {
        return "TinyPngExtension{" +
                "apiKeys='" + apiKeys + '\'' +
                ", whiteList=" + whiteList +
                ", resourceDir=" + resourceDir +
                ", enable=" + enable +
                ", resourcePattern=" + resourcePattern +
                '}';
    }
}
