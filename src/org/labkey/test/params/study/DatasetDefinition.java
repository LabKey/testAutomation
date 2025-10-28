package org.labkey.test.params.study;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.params.property.DomainProps;
import org.labkey.test.util.DomainUtils.DomainKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetDefinition extends DomainProps
{
    private final String _name;
    private String _description;
    private List<PropertyDescriptor> _fields = new ArrayList<>();
    private String _kindName;
    private Boolean _demographics;
    private Boolean _managedKeyField;
    private String _keyPropertyName;
    private Boolean _timeKeyField;

    public static final String VISIT_BASED_STUDY = DomainKind.StudyDatasetVisit.name();
    public static final String DATE_BASED_STUDY = DomainKind.StudyDatasetDate.name();

    public static DatasetDefinition create(String name)
    {
        return new DatasetDefinition(name);
    }

    public DatasetDefinition(String name)
    {
        this(name, VISIT_BASED_STUDY);
    }

    public DatasetDefinition(String name, String kindName)
    {
        _name = name;
        _kindName = kindName;
    }

    public DatasetDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public DatasetDefinition setFields(List<PropertyDescriptor> fields)
    {
        _fields = fields;
        return this;
    }

    public DatasetDefinition setKindName(String kindName)
    {
        _kindName = kindName;
        return this;
    }

    public DatasetDefinition setDemographics(Boolean demographics)
    {
        _demographics = demographics;
        return this;
    }

    public DatasetDefinition setKeyPropertyName(String keyPropertyName)
    {
        _keyPropertyName = keyPropertyName;
        return this;
    }

    public DatasetDefinition setKeyPropertyName(String keyPropertyName, Boolean managedKeyField)
    {
        _keyPropertyName = keyPropertyName;
        _managedKeyField = managedKeyField;
        return this;
    }

    public DatasetDefinition setTimeKeyField(Boolean timeKeyField)
    {
        _timeKeyField = timeKeyField;
        return this;
    }

    @Override
    protected @NotNull Domain getDomainDesign()
    {
        Domain domain = new Domain(_name);

        domain.setDescription(_description);
        domain.setFields(_fields);

        return domain;
    }

    @Override
    protected @NotNull String getKind()
    {
        return _kindName;
    }

    @Override
    protected @NotNull Map<String, Object> getOptions()
    {
        Map<String, Object> options = new HashMap<>();

        if (_demographics != null)
            options.put("demographics", _demographics);
        if (_keyPropertyName != null)
            options.put("keyPropertyName", _keyPropertyName);
        if (_managedKeyField != null)
            options.put("keyPropertyManaged", _managedKeyField);
        if (_timeKeyField != null)
            options.put("useTimeKeyField", _timeKeyField);

        return options;
    }

    @Override
    protected @NotNull String getSchemaName()
    {
        return "study";
    }

    @Override
    protected @NotNull String getQueryName()
    {
        return _name;
    }
}
