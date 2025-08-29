/*
 * Copyright (c) 2011-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jetty.util.URIUtil;
import org.labkey.test.params.FieldKey;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EscapeUtil
{
    static public String toJSONStr(String str)
    {
        if (str == null) return null;
        StringBuilder escaped = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    // Escape control characters (ASCII 0-31) and ensure Unicode compatibility
                    if (c < 32) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return "\"" + escaped + "\"";
    }

    static public String toJSONStr(Object value)
    {
        if (value instanceof String strVal)
            return toJSONStr(strVal);
        return String.valueOf(value);
    }

    static public String toJSONRow(Map<String, Object> row)
    {
        StringBuilder sb = new StringBuilder("{");
        String comma = "";
        for (Map.Entry<String, Object> entry : row.entrySet())
        {
            sb.append(comma);
            Object value = entry.getValue();
            sb.append(EscapeUtil.toJSONStr(entry.getKey()))
                    .append(": ").append(toJSONStr(value));
            comma = ",";
        }
        sb.append("}");
        return sb.toString();
    }

    static public String toJSONRow(List<Map<String, Object>> rows)
    {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Map<String, Object> row : rows)
        {
            sb.append(sep).append(toJSONRow(row));
            sep = ",";
        }

        return sb.toString();
    }

    static public String jsString(String s)
    {
        if (s == null)
            return "''";

        StringBuilder js = new StringBuilder(s.length() + 10);
        js.append("'");
        int len = s.length();
        for (int i = 0 ; i<len ; i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '\\':
                    js.append("\\\\");
                    break;
                case '\n':
                    js.append("\\n");
                    break;
                case '\r':
                    js.append("\\r");
                    break;
                case '<':
                    js.append("\\x3C");
                    break;
                case '>':
                    js.append("\\x3E");
                    break;
                case '\'':
                    js.append("\\'");
                    break;
                case '\"':
                    js.append("\\\"");
                    break;
                default:
                    js.append(c);
                    break;
            }
        }
        js.append("'");
        return js.toString();
    }

    /**
     * Encode a string to be used as a URL query key or value
     * @param s to be encoded
     * @return encoded value or empty string if provided string was `null`
     * @apiNote Use {@link #encodeUriPath(String)} for URL paths
     */
    public static String encode(String s)
    {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8)
            // Product often doesn't decode '+' into ' '. "%20" is a more reliable encoding
            .replace("+", "%20");
    }

    /**
     * Encode a string to be used as a URL path
     * For now, simply designates to URIUtil. Will replace with an impl that doesn't require Jetty utils.
     * @param path Path to be encoded
     * @return encoded value or empty string if provided string was `null`
     */
    public static String encodeUriPath(String path)
    {
        return URIUtil.encodePath(path);
    }

    /**
     * Decode a string extracted from a URL query
     * @param s to be decoded
     * @return decoded value or empty string if provided string was `null`
     * @apiNote Use {@link #decodeUriPath(String)} for URL paths
     */
    public static String decode(String s)
    {
        return null == s ? "" : URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    /**
     * Decode a string representing a URL path
     * For now, simply designates to URIUtil. Will replace with an impl that doesn't require Jetty utils.
     * @param path path to be decoded
     * @return decoded value or empty string if the provided string was `null`
     */
    public static String decodeUriPath(String path)
    {
        return URIUtil.decodePath(path);
    }

    public static String fieldKeyEncodePart(String str)
    {
        return FieldKey.encodePart(str);
    }

    public static String fieldKeyDecodePart(String str)
    {
        return FieldKey.decodePart(str);
    }

    public static String getTextChoiceValidatorExpression(List<String> options)
    {
        return options.stream()
                .map(String::trim)
                .map(value -> value.replaceAll("([\\\\|])", "\\\\$1"))
                .collect(Collectors.joining("|"));
    }

    public static String getSqlQuotedValue(String value)
    {
        return String.format("\"%s\"", value.replaceAll("\"", "\"\""));
    }

    public static String getMarkupEscapedValue(String value)
    {
        return StringEscapeUtils.escapeXml11(value);
    }

    private static final Pattern nameExpressionNeedsEscaping = Pattern.compile("([\\\\$/&}~,.])");
    public static String escapeForNameExpression(String name)
    {
        return nameExpressionNeedsEscaping.matcher(name).replaceAll("\\\\$1");
    }

}
