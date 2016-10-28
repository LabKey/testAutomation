/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
package org.labkey.test;

public enum SortDirection
{
    ASC("Ascending"),
    DESC("Descending");

    private String _string;

    SortDirection(String string)
    {
        _string = string;
    }

    @Override
    public String toString()
    {
        return _string;
    }

    public static SortDirection fromString(String str)
    {
        for (SortDirection sort : values())
        {
            if (sort.toString().equalsIgnoreCase(str) || sort.name().equalsIgnoreCase(str))
            {
                return sort;
            }
        }
        return valueOf(str);
    }
}
