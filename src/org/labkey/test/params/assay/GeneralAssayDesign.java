package org.labkey.test.params.assay;

import org.labkey.remoteapi.domain.PropertyDescriptor;

import java.util.List;

public class GeneralAssayDesign extends AssayDesign<GeneralAssayDesign>
{
    public GeneralAssayDesign(String name)
    {
        super("General", name);
    }

    public GeneralAssayDesign setBatchFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Batches", fields, keepExisting);
    }

    public GeneralAssayDesign setRunFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Runs", fields, keepExisting);
    }

    public GeneralAssayDesign setDataFields(List<PropertyDescriptor> fields, boolean keepExisting)
    {
        return setFields("Data", fields, keepExisting);
    }

    @Override
    protected GeneralAssayDesign getThis()
    {
        return this;
    }
}
