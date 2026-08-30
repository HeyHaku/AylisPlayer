

package com.aylis.Common;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtilsSerialize {

    public static String serializeIterable(CharSequence delimiter, Iterable tokens) {
        return TextUtils.join(delimiter, tokens);
    }

    public static String serializeArray(CharSequence delimiter, Object[] tokens) {
        return TextUtils.join(delimiter, tokens);
    }

    public static String[] deserializeIterable(String delimiter, String serialized) {
        return TextUtils.split(serialized, delimiter);
    }

    public static List<String> deserializeIterableAsList(String delimiter, String serialized) {
        return Arrays.asList(TextUtils.split(serialized, delimiter));
    }

    public static String serializeIterableSkipInvalidWithAdd(CharSequence delimiter, Iterable tokens) {

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token : tokens) {

            String str = token.toString();
            if (str.contains(delimiter)) continue;

            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(str);
        }
        return sb.toString();

    }

    public static String serializeIterableSkipInvalidWithAdd(CharSequence delimiter, Iterable tokens, Object addLastToken, boolean skipEmpty) {
        List<Object> addTokens = new ArrayList<>(1);
        addTokens.add(addLastToken);
        return serializeIterableSkipInvalidWithAdd(delimiter, tokens, addTokens, skipEmpty);
    }

    public static String serializeIterableSkipInvalidWithAdd(CharSequence delimiter, Iterable tokens, Iterable addLastToken, boolean skipEmpty) {

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token : tokens) {
            String str = token.toString();
            if (str.isEmpty() && skipEmpty) continue;
            if (str.contains(delimiter)) continue;

            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(str);
        }

        if (addLastToken != null) {
            for (Object token : addLastToken) {
                String str = token.toString();
                if ((str.isEmpty() && skipEmpty)) continue;

                if (!str.contains(delimiter)) {
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        sb.append(delimiter);
                    }

                    sb.append(str);
                }
            }
        }

        return sb.toString();
    }

    public static String serializeIterableSkipInvalidWithExclude(CharSequence delimiter, Iterable tokens, Object excludeToken) {
        return serializeIterableSkipInvalidWithExclude(delimiter, tokens, excludeToken, false);
    }

    public static String serializeIterableSkipInvalidWithExclude(CharSequence delimiter, Iterable tokens, Object excludeToken, boolean skipEmpty) {

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        int excludedCount = 0;
        for (Object token : tokens) {

            if (token.equals(excludeToken)) {
                excludedCount++;
                continue;
            }
            String str = token.toString();
            if (str.isEmpty() && skipEmpty) continue;
            if (str.contains(delimiter)) continue;

            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(str);
        }
        return sb.toString();

    }

}
