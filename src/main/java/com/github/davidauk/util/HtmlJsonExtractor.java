package com.github.davidauk.util;

public final class HtmlJsonExtractor {

    private HtmlJsonExtractor() {
    }

    public static String extract(String html, String key, int numChars, String stop) {
        int start = html.indexOf(key);
        if (start < 0) {
            throw new IllegalStateException("Could not find key in html: " + key);
        }

        int posBegin = start + key.length() + numChars;
        int posEnd = html.indexOf(stop, posBegin);

        if (posEnd < 0) {
            throw new IllegalStateException("Could not find stop token after key: " + key);
        }

        return html.substring(posBegin, posEnd);
    }
}
