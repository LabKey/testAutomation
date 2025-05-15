package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FieldKey implements CharSequence
{
    public static final FieldKey EMPTY = new FieldKey("");
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

    public static FieldKey fromParts(List<String> parts)
    {
        FieldKey fieldKey = EMPTY;

        for (String part : parts)
        {
            if (StringUtils.isBlank(part))
                throw new IllegalArgumentException("FieldKey contains a blank part: " + parts);
            fieldKey = fieldKey.child(part);
        }

        return fieldKey;
    }

    public static FieldKey fromParts(String... parts)
    {
        return fromParts(Arrays.asList(parts));
    }

    public static FieldKey fromFieldKey(String fieldKey)
    {
        return fromParts(Arrays.stream(fieldKey.split(SEPARATOR)).map(FieldKey::decodePart).toList());
    }

    public static FieldKey fromChars(CharSequence fieldKey)
    {
        if (fieldKey instanceof FieldKey fk)
            return fk;
        else
            return fromParts(fieldKey.toString());
    }

    private static final String[] ILLEGAL = {"$", "/", "&", "}", "~", ",", "."};
    private static final String[] REPLACEMENT = {"$D", "$S", "$A", "$B", "$T", "$C", "$P"};

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

    public FieldKey child(String name)
    {
        if (StringUtils.isBlank(getName()))
        {
            return new FieldKey(name);
        }
        else
        {
            return new FieldKey(this, name);
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

    public String[] getNameArray()
    {
        return Arrays.stream(_fieldKey.split(SEPARATOR)).map(FieldKey::decodePart).toArray(String[]::new);
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
    public final boolean equals(Object o)
    {
        if (!(o instanceof FieldKey fieldKey)) return false;

        return _fieldKey.equals(fieldKey._fieldKey);
    }

    @Override
    public int hashCode()
    {
        return _fieldKey.hashCode();
    }
}
