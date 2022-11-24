package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.InferDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.params.property.DomainProps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ListDefinition extends DomainProps
{
    private String _name;
    private String _description;
    private List<PropertyDescriptor> _fields = new ArrayList<>();
    private String _keyName;
    // API Options
    private String _titleColumn;

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

    public List<? extends PropertyDescriptor> getFields()
    {
        return new ArrayList<>(_fields); // return a copy
    }

    public ListDefinition setFields(List<? extends PropertyDescriptor> fields)
    {
        _fields = new ArrayList<>(fields); // Make sure it isn't immutable
        return this;
    }

    public ListDefinition inferFields(File dataFile, Connection connection) throws IOException, CommandException
    {
        return setFields(new InferDomainCommand(dataFile, getKind())
                .execute(connection, "/")
                .getFields());
    }

    public ListDefinition addField(@NotNull PropertyDescriptor field)
    {
        _fields.add(field);
        return this;
    }

    public String getTitleColumn()
    {
        return _titleColumn;
    }

    public ListDefinition setTitleColumn(String titleColumn)
    {
        _titleColumn = titleColumn;
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
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> options = new HashMap<>();
        options.put("name", getName());
        options.put("description", getDescription());
        options.put("keyName", getKeyName());
        if (getTitleColumn() != null)
        {
            options.put("titleColumn", getTitleColumn());
        }
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
