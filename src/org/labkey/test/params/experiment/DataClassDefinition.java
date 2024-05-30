package org.labkey.test.params.experiment;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;
import org.labkey.test.util.TestDataGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a Data Class/Data Type. Suitable for use with UI and API helpers.
 * 'exp.datas'
 */
public class DataClassDefinition extends DomainProps
{
    private String _name;
    private String _nameExpression;
    private String _description;
    private String _category;
    private String _materialSource;
    private List<FieldDefinition> _fields = new ArrayList<>();

    public DataClassDefinition(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public DataClassDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getNameExpression()
    {
        return _nameExpression;
    }

    public DataClassDefinition setNameExpression(String nameExpression)
    {
        _nameExpression = nameExpression;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public DataClassDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getCategory()
    {
        return _category;
    }

    public DataClassDefinition setCategory(String category)
    {
        _category = category;
        return this;
    }

    public String getMaterialSource()
    {
        return _materialSource;
    }

    public DataClassDefinition setMaterialSource(String materialSource)
    {
        _materialSource = materialSource;
        return this;
    }

    @NotNull
    public List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public DataClassDefinition setFields(@NotNull List<FieldDefinition> fields)
    {
        _fields = new ArrayList<>(fields);
        return this;
    }

    public DataClassDefinition addField(@NotNull FieldDefinition field)
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
        ArrayList<PropertyDescriptor> fields = new ArrayList<>(getFields());
        domain.setFields(fields);
        domain.setDescription(getDescription());
        return domain;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return "DataClass";
    }

    @NotNull
    @Override
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> options = new HashMap<>();
        options.put("name", getName());
        if (getNameExpression() != null)
        {
            options.put("nameExpression", getNameExpression());
        }
        if (getDescription() != null)
        {
            options.put("description", getDescription());
        }
        if (getCategory() != null)
        {
            options.put("category", getCategory());
        }
        if (getMaterialSource() != null)
        {
            options.put("sampleSet", getMaterialSource());
        }
        return options;
    }

    @Override
    protected @NotNull String getSchemaName()
    {
        return "exp.data";
    }

    @Override
    protected @NotNull String getQueryName()
    {
        return getName();
    }

    @Override
    public TestDataGenerator getTestDataGenerator(String containerPath)
    {
        return super.getTestDataGenerator(containerPath).withColumns(List.of(new FieldDefinition("Name", FieldDefinition.ColumnType.String)));
    }
}
