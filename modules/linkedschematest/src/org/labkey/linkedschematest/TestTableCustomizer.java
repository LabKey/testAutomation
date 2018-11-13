/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.linkedschematest;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;

import java.util.Collection;
import java.util.List;

public class TestTableCustomizer implements TableCustomizer
{
    private static final Logger _log = Logger.getLogger(TestTableCustomizer.class);

    public TestTableCustomizer()
    {
        throw new IllegalStateException("This constructor should not be called since properties are supplied in BPeopleTemplate.template.xml");
    }

    public TestTableCustomizer(MultiValuedMap props)
    {
        if (props.isEmpty())
        {
            throw new IllegalStateException("Properties not correctly passed from BPeopleTemplate.template.xml");
        }

        Collection<String> vals = props.get("testPropName");
        if (vals.size() != 1 || !"true".equals(vals.iterator().next()))
        {
            throw new IllegalStateException("Value for testPropName not correctly passed from BPeopleTemplate.template.xml: [" + StringUtils.join(vals, ";") + "]");
        }
    }

    @Override
    public void customize(TableInfo tableInfo)
    {

    }
}
