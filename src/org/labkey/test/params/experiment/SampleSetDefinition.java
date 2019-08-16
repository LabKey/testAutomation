package org.labkey.test.params.experiment;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.params.FieldDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleSetDefinition
{
    private String _name;
    private String _nameExpression;
    private String _description;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private Map<String, String> _importAliases = new HashMap<>();

    public SampleSetDefinition() { }

    public SampleSetDefinition(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public SampleSetDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getNameExpression()
    {
        return _nameExpression;
    }

    public SampleSetDefinition setNameExpression(String nameExpression)
    {
        _nameExpression = nameExpression;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public SampleSetDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    @NotNull
    public List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public SampleSetDefinition setFields(@NotNull List<FieldDefinition> fields)
    {
        _fields = new ArrayList<>(fields);
        return this;
    }

    public SampleSetDefinition addField(@NotNull FieldDefinition field)
    {
        _fields.add(field);
        return this;
    }

    @NotNull
    public Map<String, String> getImportAliases()
    {
        return _importAliases;
    }

    public SampleSetDefinition setImportAliases(@NotNull Map<String, String> importAliases)
    {
        _importAliases = new HashMap<>(importAliases);
        return this;
    }

    public SampleSetDefinition addImportAlias(@NotNull String columnName, @NotNull String sampleSetName)
    {
        _importAliases.put(columnName, sampleSetName);
        return this;
    }
}
