package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    /// Join project and folder names to create a normalized containerPath.\
    /// The resulting value will be normalized:
    ///  - repeated path separators will be collapsed to a single slash
    ///  - leading and trailing slashes will be removed
    ///  - whitespace around folder names will be removed
    ///  - the ROOT container will be represented as `null`
    /// @param pathParts project name and subfolders to be joined
    /// @return normalized container path. `null` for the root container
    public static String containerPath(String... pathParts)
    {
        return StringUtils.trimToNull(
            StringUtils.strip(String.join("/", Arrays.stream(pathParts).filter(Objects::nonNull).toList()), "/")
                .replaceAll("\\s*/+\\s*", "/"));
    }
}
