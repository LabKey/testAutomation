package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class FieldKey implements CharSequence, WrapsFieldKey
{
    private static final String[] ILLEGAL = {"$", "/", "&", "}", "~", ",", "."};
    private static final String[] REPLACEMENT = {"$D", "$S", "$A", "$B", "$T", "$C", "$P"};

    public static final FieldKey EMPTY = new FieldKey(""); // Useful as a sort of FieldKey builder starting point
    public static final FieldKey SOURCES_FK = new FieldKey("DataInputs");
    public static final FieldKey PARENTS_FK = new FieldKey("MaterialInputs");

    private static final String SEPARATOR = "/";

    private final FieldKey _parent;
    private final String _name;
    private final String _fieldKey;

    private FieldKey(String name)
    {
        _parent = null;
        _name = name;
        _fieldKey = encodePart(name);
    }

    private FieldKey(FieldKey parent, String child)
    {
        _parent = parent;
        _name = parent.getName() + SEPARATOR + child;
        _fieldKey = parent + SEPARATOR + encodePart(child);
    }

    public static List<String> getIllegalChars()
    {
        return List.of(ILLEGAL);
    }

    public static FieldKey fromParts(List<String> parts)
    {
        FieldKey fieldKey = EMPTY;

        for (String part : parts)
        {
            fieldKey = fieldKey.child(part);
        }

        return fieldKey;
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
                return fromParts(Arrays.stream(fieldKey.toString().split(SEPARATOR)).map(FieldKey::decodePart).toList());
            }
            catch (IllegalArgumentException iae)
            {
                return null;
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

    public static String encodePart(String str)
    {
        return StringUtils.replaceEach(str, ILLEGAL, REPLACEMENT);
    }

    public static String decodePart(String str)
    {
        return StringUtils.replaceEach(str, REPLACEMENT, ILLEGAL);
    }

    public FieldKey getParent()
    {
        return _parent;
    }

    public FieldKey child(String part)
    {
        if (StringUtils.isBlank(part))
            throw new IllegalArgumentException("FieldKey can't have blank part(s): " + this);

        if (StringUtils.isBlank(getName()))
        {
            return new FieldKey(part);
        }
        else
        {
            return new FieldKey(this, part);
        }
    }

    public Iterator<FieldKey> getIterator()
    {
        List<FieldKey> ancestors = new ArrayList<>();
        FieldKey temp = this;

        while (temp != null)
        {
            ancestors.add(temp);
            temp = temp.getParent();
        }

        Collections.reverse(ancestors);

        return ancestors.iterator();
    }

    public String getName()
    {
        return _name;
    }

    /**
     * Inverse of {@link #fromParts(String...)}
     * @return decoded parts of the field key
     */
    public String[] getNameArray()
    {
        return Arrays.stream(_fieldKey.split(SEPARATOR)).map(FieldKey::decodePart).toArray(String[]::new);
    }

    @Override
    public FieldKey getFieldKey()
    {
        return this;
    }

    @Override
    public @NotNull String toString()
    {
        return _fieldKey;
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
    public boolean equals(Object o)
    {
        if (!(o instanceof FieldKey fieldKey)) return false;

        return _fieldKey.equalsIgnoreCase(fieldKey._fieldKey); // FieldKeys aren't case-sensitive?
    }

    @Override
    public int hashCode()
    {
        return _fieldKey.toLowerCase().hashCode(); // FieldKeys aren't case-sensitive?
    }
}
