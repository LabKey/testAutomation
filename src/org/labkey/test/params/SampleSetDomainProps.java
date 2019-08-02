package org.labkey.test.params;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleSetDomainProps
{
    private String _name;
    private String _nameExpression;
    private String _description;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private Map<String, String> _importAliases = new HashMap<>();

    public SampleSetDomainProps() { }

    public SampleSetDomainProps(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public SampleSetDomainProps setName(String name)
    {
        _name = name;
        return this;
    }

    public String getNameExpression()
    {
        return _nameExpression;
    }

    public SampleSetDomainProps setNameExpression(String nameExpression)
    {
        _nameExpression = nameExpression;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public SampleSetDomainProps setDescription(String description)
    {
        _description = description;
        return this;
    }

    @NotNull
    public List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public SampleSetDomainProps setFields(@NotNull List<FieldDefinition> fields)
    {
        _fields = new ArrayList<>(fields);
        return this;
    }

    public SampleSetDomainProps addField(@NotNull FieldDefinition field)
    {
        _fields.add(field);
        return this;
    }

    @NotNull
    public Map<String, String> getImportAliases()
    {
        return _importAliases;
    }

    public SampleSetDomainProps setImportAliases(@NotNull Map<String, String> importAliases)
    {
        _importAliases = new HashMap<>(importAliases);
        return this;
    }

    public SampleSetDomainProps addImportAlias(@NotNull String columnName, @NotNull String sampleSetName)
    {
        _importAliases.put(columnName, sampleSetName);
        return this;
    }
}
