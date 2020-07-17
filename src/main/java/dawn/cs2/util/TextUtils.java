package dawn.cs2.util;

public class TextUtils {

    public static String quote(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i = 0; i < str.length(); i++) {
            char c;
            switch (c = str.charAt(i)) {
                case '\0':
                    result.append("\\0");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\"':
                    result.append("\\\"");
                    break;
                default:
                    if (c >= 32 && c < 127)
                        result.append(str.charAt(i));
                    else {
                        String hex = Integer.toHexString(c);
                        result.append("\\u0000".substring(0, 6 - hex.length())).append(hex);
                    }
            }
        }
        return result.append("\"").toString();
    }

    public static String quote(char c) {
        switch (c) {
            case '\0':
                return "\'\\0\'";
            case '\t':
                return "\'\\t\'";
            case '\n':
                return "\'\\n\'";
            case '\r':
                return "\'\\r\'";
            case '\\':
                return "\'\\\\\'";
            case '\"':
                return "\'\\\"\'";
            case '\'':
                return "\'\\\'\'";
        }
        if (c >= 32 && c < 127)
            return "\'" + c + "\'";
        else {
            String hex = Integer.toHexString(c);
            return "\'\\u0000".substring(0, 7 - hex.length()) + hex + "\'";
        }
    }

    public static String unescapeUnicode(String escaped) {
        if (!escaped.contains("\\u"))
            return escaped;

        StringBuilder processed = new StringBuilder();

        int position = escaped.indexOf("\\u");
        while (position != -1) {
            if (position != 0)
                processed.append(escaped.substring(0, position));
            String token = escaped.substring(position + 2, position + 6);
            escaped = escaped.substring(position + 6);
            processed.append((char) Integer.parseInt(token, 16));
            position = escaped.indexOf("\\u");
        }
        processed.append(escaped);

        return processed.toString();
    }

}
