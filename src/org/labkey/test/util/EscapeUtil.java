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

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * UNDONE: Refactor useful methods from PageFlowUtil into util.jar that can be used by the test harness then delete this class.
 */
public class EscapeUtil
{
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
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static String decode(String s)
    {
        return null==s ? "" : URLDecoder.decode(s, StandardCharsets.UTF_8);
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
