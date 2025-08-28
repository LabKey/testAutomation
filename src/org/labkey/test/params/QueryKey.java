package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class QueryKey<T extends QueryKey<T>>
{
    private static final String[] ILLEGAL = {"$", "/", "&", "}", "~", ",", "."};
    private static final String[] REPLACEMENT = {"$D", "$S", "$A", "$B", "$T", "$C", "$P"};

    private final T _parent;
    private final String _name;
    private final String _encodedKey;

    protected QueryKey(T parent, String name)
    {
        _parent = parent;
        _name = name;

        if (parent != null && !parent.getName().isEmpty())
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
