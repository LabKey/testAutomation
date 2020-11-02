package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListDefinition extends DomainProps
{
    private static final String DOMAIN_KIND = "VarList";

    private String _name;
    private String _description;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private String _keyName;

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

    public String getKeyName()
    {
        return _keyName;
    }

    public ListDefinition setKeyName(String keyName)
    {
        _keyName = keyName;
        return this;
    }

    public List<FieldDefinition> getFields()
    {
        return new ArrayList<>(_fields); // return a copy
    }

    public ListDefinition setFields(List<FieldDefinition> fields)
    {
        _fields = new ArrayList<>(fields); // Make sure it isn't immutable
        return this;
    }

    public ListDefinition addField(@NotNull FieldDefinition field)
    {
        _fields.add(field);
        return this;
    }

    /*
    DomainProps
     */

    @NotNull
    @Override
    protected Domain getDomainDesign()
    {
        Domain domain = new Domain(getName());
        domain.setFields(new ArrayList<>(getFields()));
        domain.setDescription(getDescription());
        return domain;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return DOMAIN_KIND;
    }

    @NotNull
    @Override
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> options = new HashMap<>();
        options.put("name", getName());
        options.put("description", getDescription());
        options.put("keyName", getKeyName());
        return options;
    }

    @Override
    protected @NotNull String getSchemaName()
    {
        return "lists";
    }

    @Override
    protected @NotNull String getQueryName()
    {
        return getName();
    }
}
