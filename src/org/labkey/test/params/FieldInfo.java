package org.labkey.test.params;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable alternative to 'FieldDefinition'
 * Use this for shared global field information
 */
public class FieldInfo implements WrapsFieldKey
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

    @Override
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
        return getFieldDefinition(_columnType);
    }

    public FieldDefinition getFieldDefinition(String lookupContainerPath)
    {
        if (!_columnType.isLookup())
        {
            throw new IllegalArgumentException("Unable to set lookup container for %s column: %s".formatted(_columnType.getLabel(), getName()));
        }
        else
        {
            String schema = _columnType.getLookupInfo().getSchema();
            String table = _columnType.getLookupInfo().getTable();
            FieldDefinition.ColumnType columnType = _columnType.getRangeURI().equals(FieldDefinition.ColumnType.Integer.getRangeURI())
                ? new FieldDefinition.IntLookup(lookupContainerPath, schema, table)
                : new FieldDefinition.StringLookup(lookupContainerPath, schema, table);
            return getFieldDefinition(columnType);
        }
    }

    private FieldDefinition getFieldDefinition(FieldDefinition.ColumnType columnType)
    {
        FieldDefinition fieldDefinition = new FieldDefinition(getName(), columnType);
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
