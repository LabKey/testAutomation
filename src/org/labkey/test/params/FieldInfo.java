package org.labkey.test.params;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.CachingSupplier;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TextUtils;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable alternative to 'FieldDefinition'
 * Use this for shared global field information
 * Implements CharSequence to be compatible with grid components
 */
public class FieldInfo implements CharSequence, WrapsFieldKey
{
    private final FieldKey _fieldKey;
    private final String _rawLabel;
    private final ColumnType _columnType;
    private final Consumer<FieldDefinition> _fieldDefinitionMutator;
    private final String _namePart; // used for random field generation to track the name part used
    private final CachingSupplier<String> _label = new CachingSupplier<>(() -> Objects.requireNonNullElseGet(getRawLabel(), () -> FieldDefinition.labelFromName(getName())));

    private FieldInfo(FieldKey fieldKey, String label, ColumnType columnType, Consumer<FieldDefinition> fieldDefinitionMutator, String namePart)
    {
        _fieldKey = fieldKey;
        _rawLabel = label;
        _columnType = Objects.requireNonNullElse(columnType, ColumnType.String);
        _fieldDefinitionMutator = fieldDefinitionMutator;
        _namePart = namePart;
    }

    public FieldInfo(String name, String label, ColumnType columnType)
    {
        this(FieldKey.fromParts(name.trim()), label, columnType, null, name);
    }

    public FieldInfo(String name, String label)
    {
        this(name, label, null);
    }

    public FieldInfo(String name, ColumnType columnType)
    {
        this(name, null, columnType);
    }

    public FieldInfo(String name)
    {
        this(name, null, null);
    }

    /**
     * Creates a FieldInfo with a semi-random name
     */
    public static FieldInfo random(String namePart, ColumnType columnType, DomainUtils.DomainKind domainKind, Integer maxLength)
    {
        return new FieldInfo(FieldKey.fromParts(TestDataGenerator.randomFieldName(namePart, domainKind, maxLength)), null, columnType, null, namePart);
    }

    /**
     * Creates a FieldInfo with a semi-random name
     */
    public static FieldInfo random(String namePart, ColumnType columnType, DomainUtils.DomainKind domainKind)
    {
        return random(namePart, columnType, domainKind, null);
    }

    /**
     * Creates a FieldInfo with a semi-random name
     */
    public static FieldInfo random(String namePart, ColumnType columnType, int maxLength)
    {
        return random(namePart, columnType, null, maxLength);
    }

    /**
     * Creates a FieldInfo with a semi-random name
     */
    public static FieldInfo random(String namePart, ColumnType columnType)
    {
        return random(namePart, columnType, null);
    }

    /**
     * Creates a String field with a semi-random name
     */
    public static FieldInfo random(String namePart)
    {
        return random(namePart, null);
    }

    /**
     *
     * @param fieldDefinitionMutator will be invoked by {@link #getFieldDefinition()}
     * @return a new FieldInfo with the provided mutator. Any existing mutator will be replaced.
     */
    @Contract(pure = true)
    public FieldInfo customizeFieldDefinition(Consumer<FieldDefinition> fieldDefinitionMutator)
    {
        FieldDefinition verifier = new FieldDefinition("temp", _columnType);
        fieldDefinitionMutator.accept(verifier);
        if (verifier.getLabel() != null)
        {
            throw new IllegalArgumentException("FieldDefinition customizer should not modify field label");
        }
        return new FieldInfo(_fieldKey, _rawLabel, _columnType, fieldDefinitionMutator, _namePart);
    }

    @Contract(pure = true)
    protected String getRawLabel()
    {
        return _rawLabel;
    }

    @Contract(pure = true)
    public String getLabel()
    {
        return _label.get();
    }

    /**
     * Get field label as it appears when rendered in browser
     */
    @Contract(pure = true)
    public String getUiLabel()
    {
        return TextUtils.normalizeSpace(getLabel());
    }

    @Override
    @Contract(pure = true)
    public FieldKey getFieldKey()
    {
        return _fieldKey;
    }

    @Contract(pure = true)
    public String getName()
    {
        return _fieldKey.getName();
    }

    /**
     * Get column name quoted for use in queries and calculated field expressions
     */
    @Contract(pure = true)
    public String getSqlName()
    {
        return EscapeUtil.getSqlQuotedValue(_fieldKey.getName());
    }

    /**
     * Get name escaped for use in sample or source name expressions
     */
    @Contract(pure = true)
    public String getExpName()
    {
        return EscapeUtil.escapeForNameExpression(getName());
    }

    @Contract(pure = true)
    public FieldKey child(String name)
    {
        return _fieldKey.child(name);
    }

    /**
     * @return A FieldDefinition to be used for domain creation
     */
    @Contract(pure = true)
    public FieldDefinition getFieldDefinition()
    {
        return getFieldDefinition(_columnType);
    }

    /**
     * Shared lookup definitions might want to customize the target table.
     * @param lookupContainerPath the containerPath to use for the lookup
     * @return A FieldDefinition to be used for domain creation
     */
    @Contract(pure = true)
    public FieldDefinition getFieldDefinition(String lookupContainerPath)
    {
        if (!_columnType.isLookup())
        {
            throw new IllegalArgumentException("Unable to set lookup container for %s column: %s".formatted(_columnType.getLabel(), _fieldKey.getName()));
        }
        else
        {
            String schema = _columnType.getLookupInfo().getSchema();
            String table = _columnType.getLookupInfo().getTable();
            ColumnType columnType = _columnType.getRangeURI().equals(ColumnType.Integer.getRangeURI())
                ? new FieldDefinition.IntLookup(lookupContainerPath, schema, table)
                : new FieldDefinition.StringLookup(lookupContainerPath, schema, table);
            return getFieldDefinition(columnType);
        }
    }

    private FieldDefinition getFieldDefinition(ColumnType columnType)
    {
        FieldDefinition fieldDefinition = new FieldDefinition(_fieldKey.getName(), columnType);
        if (getRawLabel() != null)
        {
            fieldDefinition.setLabel(getRawLabel());
        }
        if (_fieldDefinitionMutator != null)
        {
            _fieldDefinitionMutator.accept(fieldDefinition);
        }
        if (_namePart != null)
        {
            fieldDefinition.setNamePart(_namePart);
        }
        return fieldDefinition;
    }

    @Override
    public int length()
    {
        return _fieldKey.length();
    }

    @Override
    public char charAt(int index)
    {
        return _fieldKey.charAt(index);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end)
    {
        return _fieldKey.subSequence(start, end);
    }

    @Override
    public @NotNull String toString()
    {
        return _fieldKey.toString();
    }
}
