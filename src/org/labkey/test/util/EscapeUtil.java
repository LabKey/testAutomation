/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UNDONE: Refactor useful methods from PageFlowUtil into util.jar that can be used by the test harness then delete this class.
 */
public class EscapeUtil
{
    private static final Pattern urlPatternStart = Pattern.compile("((http|https|ftp|mailto)://\\S+).*");

    static public final String NONPRINTING_ALTCHAR = "~";

    // From StringUtilsLabKey
    /** Recognizes strings that start with http://, https://, ftp://, or mailto: */
    public static boolean startsWithURL(String s)
    {
        if (s == null)
        {
            return false;
        }
        s = s.toLowerCase();
        return s.startsWith("http://") || s.startsWith("https://") || s.startsWith("ftp://") || s.startsWith("mailto:");
    }

    static public String filter(String s, boolean encodeSpace, boolean encodeLinks)
    {
        if (null == s || 0 == s.length())
            return "";

        int len = s.length();
        StringBuilder sb = new StringBuilder(2 * len);
        boolean newline = false;

        for (int i=0 ; i < len; ++i)
        {
            char c = s.charAt(i);

            if (!Character.isWhitespace(c))
                newline = false;
            else if ('\r' == c || '\n' == c)
                newline = true;

            switch (c)
            {
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#039;");    // works for xml and html
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\n':
                    if (encodeSpace)
                        sb.append("<br>\n");
                    else
                        sb.append(c);
                    break;
                case '\r':
                    break;
                case '\t':
                    if (!encodeSpace)
                        sb.append(c);
                    else if (newline)
                        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    else
                        sb.append("&nbsp; &nbsp; ");
                    break;
                case ' ':
                    if (encodeSpace && newline)
                        sb.append("&nbsp;");
                    else
                        sb.append(' ');
                    break;
                case 'f':
                case 'h':
                case 'm':
                    if (encodeLinks)
                    {
                        String sub = s.substring(i);
                        if ((c == 'f' || c == 'h' || c == 'm') && startsWithURL(sub))
                        {
                            Matcher m = urlPatternStart.matcher(sub);
                            if (m.find())
                            {
                                String href = m.group(1);
                                if (href.endsWith("."))
                                    href = href.substring(0, href.length() - 1);
                                // for html/xml careful of " and "> and "/>
                                int lastQuote = Math.max(href.lastIndexOf("\""),href.lastIndexOf("\'"));
                                if (lastQuote >= href.length()-3)
                                    href = href.substring(0, lastQuote);
                                String filterHref = filter(href, false, false);
                                sb.append("<a href=\"").append(filterHref).append("\">").append(filterHref).append("</a>");
                                i += href.length() - 1;
                                break;
                            }
                        }
                    }
                    sb.append(c);
                    break;
                default:
                    if (c >= ' ')
                        sb.append(c);
                    else
                    {
                        if (c == 0x08) // backspace (e.g. xtandem output)
                            break;
                        sb.append(NONPRINTING_ALTCHAR);
                    }
                    break;
            }
        }

        return sb.toString();
    }


    static public String h(String s)
    {
        return filter(s);
    }

    public static String filter(Object o)
    {
        return filter(o == null ? null : o.toString());
    }

    /**
     * HTML encode a string
     */
    public static String filter(String s)
    {
        return filter(s, false, false);
    }


    static public String filter(String s, boolean translateWhiteSpace)
    {
        return filter(s, translateWhiteSpace, false);
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

    public static String encode(String s)
    {
        if (s == null)
            return "";
        try
        {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        }
        catch (UnsupportedEncodingException x)
        {
            throw new RuntimeException(x);
        }
    }

    public static String decode(String s)
    {
        try
        {
            return null==s ? "" : URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException x)
        {
            throw new RuntimeException(x);
        }
    }

    public static String fieldKeyEncodePart(String str)
    {
        str = StringUtils.replace(str, "$", "$D");
        str = StringUtils.replace(str, "/", "$S");
        str = StringUtils.replace(str, "&", "$A");
        str = StringUtils.replace(str, "}", "$B");
        str = StringUtils.replace(str, "~", "$T");
        str = StringUtils.replace(str, ",", "$C");
        return str;
    }

    public static String fieldKeyDecodePart(String str)
    {
        str = StringUtils.replace(str, "$C", ",");
        str = StringUtils.replace(str, "$T", "~");
        str = StringUtils.replace(str, "$B", "}");
        str = StringUtils.replace(str, "$A", "&");
        str = StringUtils.replace(str, "$S", "/");
        str = StringUtils.replace(str, "$D", "$");
        return str;
    }
}
