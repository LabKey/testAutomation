package org.labkey.test.util;

import java.util.List;
import java.util.regex.Pattern;

public class TextUtils
{
    private static final Pattern NS_PATTERN = Pattern.compile("\\s+");

    private TextUtils() {}

    /**
     * Equivalent to XPath {@code normalize-space()}:<br>
     * "The normalize-space function strips leading and trailing white-space from a string, replaces sequences of
     * whitespace characters by a single space, and returns the resulting string."
     */
    public static String normalizeSpace(String value)
    {
        if (value == null)
            return value;
        else
            return NS_PATTERN.matcher(value).replaceAll(" ").trim();
    }

    public static List<String> normalizeSpace(List<String> values)
    {
        return values.stream().map(TextUtils::normalizeSpace).toList();
    }

    public static String normalizeSpaceMultiline(String value)
    {
        String[] lines = value.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
            lines[i] = normalizeSpace(lines[i]);
        }
        return String.join("\n", lines);
    }

    public static List<String> normalizeSpaceMultiline(List<String> values)
    {
        return values.stream().map(TextUtils::normalizeSpaceMultiline).toList();
    }
}
