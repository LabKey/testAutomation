package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Expand stub list definition and refactor list helpers to use it
 */
public class ListDefinition extends DomainProps
{
    private static final String AUTO_INCREMENT_KIND = "IntList";
    private static final String DOMAIN_KIND = "VarList";

    private String _name;
    private String _description;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private boolean _autoIncrementKey = true;

    public ListDefinition(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public ListDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public ListDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public ListDefinition setFields(List<FieldDefinition> fields)
    {
        _fields = fields;
        return this;
    }

    public ListDefinition addField(@NotNull FieldDefinition field)
    {
        _fields.add(field);
        return this;
    }

    @NotNull
    @Override
    protected Domain getDomainDesign()
    {
        Domain domain = new Domain(getName());
        ArrayList<PropertyDescriptor> fields = new ArrayList<>(getFields());
        if (_autoIncrementKey)
        {
            fields.add(new FieldDefinition("Key", FieldDefinition.ColumnType.Integer).setPrimaryKey(true));
        }
        domain.setFields(fields);
        domain.setDescription(getDescription());
        return domain;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return _autoIncrementKey ? AUTO_INCREMENT_KIND : DOMAIN_KIND;
    }

    @NotNull
    @Override
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> json = new HashMap<>();
        json.put("name", getName());
        json.put("description", getDescription());
        return json;
    }
}
