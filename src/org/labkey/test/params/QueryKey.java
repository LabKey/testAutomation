package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class QueryKey<T extends QueryKey<T>>
{
    private static final String[] ILLEGAL = {"$", "/", "&", "}", "~", ",", "."};
    private static final String[] REPLACEMENT = {"$D", "$S", "$A", "$B", "$T", "$C", "$P"};

    private final T _parent;
    private final String _name;
    private final String _encodedKey;

    protected QueryKey(T parent, @NotNull String name)
    {
        _name = Objects.requireNonNull(name);
        _parent = parent;

        if (parent != null)
        {
            _encodedKey = parent + getDivider() + encodePart(name);
        }
        else
        {
            _encodedKey = encodePart(name);
        }
    }

    protected abstract String getDivider();

    protected abstract T getThis();

    protected static <T> T fromParts(BiFunction<T, String, T> factory, List<String> parts)
    {
        T fieldKey = null;
        for (String part : parts)
        {
            fieldKey = factory.apply(fieldKey, part);
        }
        return fieldKey;
    }

    public static List<String> getIllegalChars()
    {
        return List.of(ILLEGAL);
    }

    public static String encodePart(String str)
    {
        return StringUtils.replaceEach(str, ILLEGAL, REPLACEMENT);
    }

    public static String decodePart(String str)
    {
        return StringUtils.replaceEach(str, REPLACEMENT, ILLEGAL);
    }

    public T getParent()
    {
        return _parent;
    }

    public Iterator<T> getIterator()
    {
        List<T> ancestors = new ArrayList<>();
        T temp = getThis();

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

    public String getFullName()
    {
        return decodePart(_encodedKey);
    }

    @Override
    public @NotNull String toString()
    {
        return _encodedKey;
    }

}
