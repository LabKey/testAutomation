package org.labkey.test.util;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 6/21/12
 * Time: 11:09 AM
 */
public class ComponentQuery
{
    public static String fromAttributes(String xtype, Map<String, String> attrs)
    {
        StringBuilder sb = new StringBuilder(xtype);
        for (String attrName : attrs.keySet())
        {
            sb.append("[" + attrName + "=\"" + attrs.get(attrName) + "\"]");
        }
        return sb.toString();
    }
}
