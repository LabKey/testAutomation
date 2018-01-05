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
