package org.labkey.test.params.property;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.params.FieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomainPropsImpl extends DomainProps
{
    private final String _kind;
    private final String _schemaName;
    private final String _queryName;

    private List<FieldDefinition> _fields = new ArrayList<>();
    private Map<String, Object> _options;
    private String _description;

    public DomainPropsImpl(String kind, String schemaName, String queryName)
    {
        _kind = kind;
        _schemaName = schemaName;
        _queryName = queryName;
    }

    @Override
    protected @NotNull String getKind()
    {
        return _kind;
    }

    @Override
    protected @NotNull String getSchemaName()
    {
        return _schemaName;
    }

    @Override
    protected @NotNull String getQueryName()
    {
        return _queryName;
    }

    @Override
    protected @NotNull Map<String, Object> getOptions()
    {
        return _options;
    }

    public DomainPropsImpl setOptions(Map<String, Object> options)
    {
        _options = options;
        return this;
    }

    protected List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public DomainPropsImpl setFields(List<FieldDefinition> fields)
    {
        _fields = fields;
        return this;
    }

    protected String getDescription()
    {
        return _description;
    }

    public DomainPropsImpl setDescription(String description)
    {
        _description = description;
        return this;
    }

    @NotNull
    @Override
    protected Domain getDomainDesign()
    {
        Domain domain = new Domain(getQueryName());
        ArrayList<PropertyDescriptor> fields = new ArrayList<>(getFields());
        domain.setFields(fields);
        domain.setDescription(_description);
        return domain;
    }

}
