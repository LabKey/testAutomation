/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import java.util.Map;

public class ComponentQuery
{
    public static String fromAttributes(String xtype, Map<String, String> attrs)
    {
        StringBuilder sb = new StringBuilder(xtype);
        for (String attrName : attrs.keySet())
        {
            sb.append("[").append(attrName).append("=\"").append(attrs.get(attrName)).append("\"]");
        }
        return sb.toString();
    }
}
