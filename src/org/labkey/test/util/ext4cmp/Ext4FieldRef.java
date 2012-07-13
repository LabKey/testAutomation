/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.util.ext4cmp;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Ext4Helper;

/**
 * User: bbimber
 * Date: 6/20/12
 * Time: 12:40 PM
 */
public class Ext4FieldRef extends Ext4CmpRef
{
    public Ext4FieldRef(String id, BaseSeleniumWebTest test)
    {
        super(id, test);
    }

    public static Ext4FieldRef getForLabel(BaseSeleniumWebTest test, String label)
    {
        return Ext4Helper.queryOne(test, "field[fieldLabel^=\"" + label + "\"]", Ext4FieldRef.class);
    }

    public String setValue(String val)
    {
        val = val.replaceAll("'", "\"");  //escape single quotes
        return eval("this.setValue(\"" + val + "\")");
    }

    public String setValue(String[] vals)
    {
        String query = "this.setValue([";
        for (String val : vals)
        {
            query += "\"" + val + "\",";
        }
        query = query.substring(0, query.length()-1) +  "])"; //cut the trailing comma off
        return eval(query);
    }


    public String getValue()
    {
        return eval("this.getValue()");
    }
}
