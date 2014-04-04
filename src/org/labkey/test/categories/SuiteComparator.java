/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test.categories;

import java.util.Comparator;

public class SuiteComparator implements Comparator
{
    @Override
    public int compare(Object o1, Object o2)
    {
        Class suite1 = (Class)o1;
        Class suite2 = (Class)o2;
        return suite1.getSimpleName().compareTo(suite2.getSimpleName());
    }
}
