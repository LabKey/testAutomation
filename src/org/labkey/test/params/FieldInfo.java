package org.labkey.test.params;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable alternative to 'FieldDefinition'
 * Use this for shared global field information
 */
public class FieldInfo
{
    private final FieldKey _fieldKey;
    private final String _label;
    private final FieldDefinition.ColumnType _columnType;
    private final Consumer<FieldDefinition> _fieldDefinitionMutator;

    private FieldInfo(FieldKey fieldKey, String label, FieldDefinition.ColumnType columnType, Consumer<FieldDefinition> fieldDefinitionMutator)
    {
        _fieldKey = fieldKey;
        _label = label;
        _columnType = Objects.requireNonNullElse(columnType, FieldDefinition.ColumnType.String);
        _fieldDefinitionMutator = fieldDefinitionMutator;
    }

    public FieldInfo(String name, String label, FieldDefinition.ColumnType columnType)
    {
        this(FieldKey.fromParts(name.trim()), label, columnType, null);
    }

    public FieldInfo(String name, String label)
    {
        this(name, label, null);
    }

    public FieldInfo(String name, FieldDefinition.ColumnType columnType)
    {
        this(name, null, columnType);
    }

    public FieldInfo(String name)
    {
        this(name, null, null);
    }

    public FieldInfo customizeFieldDefinition(Consumer<FieldDefinition> fieldDefinitionMutator)
    {
        return new FieldInfo(_fieldKey, _label, _columnType, fieldDefinitionMutator);
    }

    protected String getRawLabel()
    {
        return _label;
    }

    public String getLabel()
    {
        return Objects.requireNonNullElseGet(getRawLabel(), () -> FieldDefinition.labelFromName(_fieldKey.getName()));
    }

    public FieldKey getFieldKey()
    {
        return _fieldKey;
    }

    public String getName()
    {
        return _fieldKey.getName();
    }

    public FieldKey child(String name)
    {
        return _fieldKey.child(name);
    }

    public FieldDefinition getFieldDefinition()
    {
        FieldDefinition fieldDefinition = new FieldDefinition(getName(), _columnType);
        if (getRawLabel() != null)
        {
            fieldDefinition.setLabel(getRawLabel());
        }
        if (_fieldDefinitionMutator != null)
        {
            _fieldDefinitionMutator.accept(fieldDefinition);
        }
        return fieldDefinition;
    }
}
