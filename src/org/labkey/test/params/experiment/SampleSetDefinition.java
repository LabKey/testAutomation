package org.labkey.test.params.experiment;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleSetDefinition extends DomainProps
{
    private String _name;
    private String _nameExpression;
    private String _description;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private Map<String, String> _parentAliases = new HashMap<>();

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
    public Map<String, String> getParentAliases()
    {
        return _parentAliases;
    }

    public SampleSetDefinition setParentAliases(@NotNull Map<String, String> parentAliases)
    {
        _parentAliases = new HashMap<>(parentAliases);
        return this;
    }

    public SampleSetDefinition addParentAlias(@NotNull String columnName, String sampleSetName)
    {
        _parentAliases.put(columnName, sampleSetName);
        return this;
    }

    public SampleSetDefinition addParentAlias(@NotNull String columnName)
    {
        _parentAliases.put(columnName, SampleTypeDesigner.CURRENT_SAMPLE_TYPE);
        return this;
    }

    @NotNull
    @Override
    protected Domain getDomainDesign()
    {
        Domain domain = new Domain(getName());
        ArrayList<PropertyDescriptor> fields = new ArrayList<>(getFields());
        fields.add(new PropertyDescriptor("Name", null));
        domain.setFields(fields);
        domain.setDescription(getDescription());
        return domain;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return "SampleSet";
    }

    @NotNull
    @Override
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> json = new HashMap<>();
        json.put("name", getName());
        json.put("nameExpression", getNameExpression());
        if (!getParentAliases().isEmpty())
        {
            Map<String, String> importAliases = new HashMap<>();
            for (String columnName : getParentAliases().keySet())
            {
                String sampleType = getParentAliases().get(columnName);
                if (sampleType == null || sampleType.equals(SampleTypeDesigner.CURRENT_SAMPLE_TYPE))
                {
                    sampleType = getName();
                }
                String aliasTable = "materialInputs/" + sampleType;
                importAliases.put(columnName, aliasTable);
            }
            json.put("importAliases", importAliases);
        }
        return json;
    }
}
