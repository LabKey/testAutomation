package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class FieldKey extends QueryKey<FieldKey> implements CharSequence, WrapsFieldKey
{
    public static final FieldKey SOURCES_FK = FieldKey.fromParts("DataInputs");
    public static final FieldKey PARENTS_FK = FieldKey.fromParts("MaterialInputs");

    private static final String DIVIDER = "/";

    private FieldKey(FieldKey parent, String child)
    {
        super(parent, child);
    }

    public static FieldKey fromParts(List<String> parts)
    {
        return QueryKey.fromParts(FieldKey::new, parts);
    }

    public static FieldKey fromParts(String... parts)
    {
        return fromParts(Arrays.asList(parts));
    }

    /**
     * Construct a FieldKey from a CharSequence that might be an encoded fieldKey
     * @param fieldKey String or FieldKey
     * @return FieldKey representation of the String, or the identity if a FieldKey was provided
     */
    public static @Nullable FieldKey fromFieldKey(CharSequence fieldKey)
    {
        if (fieldKey instanceof WrapsFieldKey fk)
        {
            return fk.getFieldKey();
        }
        else
        {
            try
            {
                return fromParts(Arrays.stream(fieldKey.toString().split(DIVIDER)).map(FieldKey::decodePart).toList());
            }
            catch (IllegalArgumentException iae)
            {
                return null; // FieldReferenceManager depends on this returning null.
            }
        }
    }

    /**
     * Construct a FieldKey from a CharSequence that might be a field name
     * @param nameOrFieldKey unencoded field name or an existing FieldKey object
     * @return fieldKey encoded name, or the identity if one was provided
     */
    public static FieldKey fromName(CharSequence nameOrFieldKey)
    {
        if (nameOrFieldKey instanceof WrapsFieldKey fk)
            return fk.getFieldKey();
        else
            return fromParts(nameOrFieldKey.toString());
    }

    public FieldKey child(String... parts)
    {
        return child(Arrays.asList(parts));
    }

    public FieldKey child(List<String> parts)
    {
        FieldKey child = this;

        for (String part : parts)
        {
            if (StringUtils.isBlank(part))
                throw new IllegalArgumentException("FieldKey can't have blank part(s): " + parts);

            child = new FieldKey(child, part);
        }
        return child;
    }

    // QueryKey

    @Override
    protected String getDivider()
    {
        return DIVIDER;
    }

    @Override
    protected FieldKey getThis()
    {
        return this;
    }

    // WrapsFieldKey

    @Override
    public FieldKey getFieldKey()
    {
        return this;
    }

    // CharSequence

    @Override
    public int length()
    {
        return toString().length();
    }

    @Override
    public char charAt(int index)
    {
        return toString().charAt(index);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end)
    {
        return toString().subSequence(start, end);
    }

    // Object

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof FieldKey fieldKey)) return false;

        return toString().equalsIgnoreCase(fieldKey.toString()); // FieldKeys aren't case-sensitive?
    }

    @Override
    public int hashCode()
    {
        return toString().toLowerCase().hashCode(); // FieldKeys aren't case-sensitive?
    }
}
