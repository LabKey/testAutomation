package org.labkey.serverapi.reader;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.beanutils.ConversionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FieldKey extends QueryKey<FieldKey>
{
    private static final String DIVIDER = "/";

    private static final QueryKey.Factory<FieldKey> FACTORY = FieldKey::new;

    /**
     * same as fromString() but URL encoded
     */
    static public FieldKey decode(String str)
    {
        return QueryKey.decode(FACTORY, DIVIDER, str);
    }


    /**
     * Construct a FieldKey from a string that may have been returned by ColumnInfo.getName()
     * or by FieldKey.toString(), or from an URL filter.
     * Try to avoid calling this on strings that are hard-coded in the source code.
     * Use FieldKey.fromParts(...) instead.  That version handles escaping the individual
     * parts of the FieldKey, and will enable us to maintain flexibility to change the
     * escaping algorithm.
     */
    static public FieldKey fromString(String str)
    {
        return QueryKey.fromString(FACTORY, DIVIDER, str);
    }

    static public FieldKey fromString(FieldKey parent, String str)
    {
        return QueryKey.fromString(FACTORY, DIVIDER, parent, str);
    }

    @JsonCreator
    static public FieldKey fromParts(List<String> parts)
    {
        return QueryKey.fromParts(FACTORY, parts);
    }

    static public FieldKey fromParts(String... parts)
    {
        return fromParts(Arrays.asList(parts));
    }

    static public FieldKey fromParts(Enum... parts)
    {
        List<String> strings = new ArrayList<>(parts.length);
        for (Enum part : parts)
        {
            strings.add(part.toString());
        }
        return fromParts(strings);
    }


    static public FieldKey fromParts(FieldKey... parts)
    {
        return QueryKey.fromParts(FACTORY, parts);
    }


    static public FieldKey fromPath(Path path)
    {
        List<String> strings = new ArrayList<>(path.size());
        for (String part : path)
            strings.add(part);
        return fromParts(strings);
    }


    static public FieldKey remap(FieldKey key, @Nullable FieldKey parent, @Nullable Map<FieldKey, FieldKey> remap)
    {
        FieldKey replace = remap == null ? null : remap.get(key);
        if (null != replace)
            return replace;
        else if (null != parent)
            return FieldKey.fromParts(parent, key);
        return key;
    }

    static public boolean needsEncoding(String str)
    {
        return QueryKey.needsEncoding(str, DIVIDER);
    }


    public FieldKey(@Nullable FieldKey parent, @NotNull String name)
    {
        super(parent, name);
    }

    public FieldKey(@Nullable FieldKey parent, Enum name)
    {
        super(parent, name);
    }

    @Override
    protected String getDivider()
    {
        return DIVIDER;
    }

    public FieldKey getTable()
    {
        return (FieldKey) super.getParent();
    }

    public String getRootName()
    {
        FieldKey fk = this;
        while (null != fk.getParent())
            fk = fk.getParent();
        return fk.getName();
    }

    @Override
    public FieldKey getParent()
    {
        return (FieldKey) super.getParent();
    }

    public @NotNull String getLabel()
    {
        return getName();
    }

//    public @NotNull String getCaption()
//    {
//        return ColumnInfo.labelFromName(getName());
//    }

    public boolean isAllColumns()
    {
        return false;
        // return getName().equals("*");
    }

    /**
     * Remove the root component from this key if it matches the rootPart, otherwise null.
     *
     * @param rootPart The root FieldKey name to match.
     * @return The new FieldKey with the root key removed
     */
    @Nullable
    public FieldKey removeParent(String rootPart)
    {
        List<String> parts = getParts();
        if (parts.size() > 1 && parts.get(0).equalsIgnoreCase(rootPart))
        {
            parts = parts.subList(1, parts.size());
            return FieldKey.fromParts(parts);
        }

        return null;
    }

    /**
     * Create a new child FieldKey of this FieldKey using <code>parts</code>
     */
    public FieldKey append(String... parts)
    {
        FieldKey ret = this;
        for (String part : parts)
        {
            ret = new FieldKey(ret, part);
        }
        return ret;
    }

    public int compareTo(FieldKey o)
    {
        return CASE_INSENSITIVE_ORDER.compare(this, o);
    }

    public static final Comparator<FieldKey> CASE_INSENSITIVE_STRING_ORDER = (a, b) -> {
        if (a == b) return 0;
        if (null == a) return -1;
        if (null == b) return 1;
        return String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString());
    };


    public static final Comparator<FieldKey> CASE_INSENSITIVE_ORDER = new Comparator<FieldKey>()
    {
        @Override
        public int compare(FieldKey a, FieldKey b)
        {
            if (a == b) return 0;
            if (null == a) return -1;
            if (null == b) return 1;
            int c = compare(a.getParent(), b.getParent());
            return c != 0 ? c : String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
        }
    };

    public static final Comparator<FieldKey> CASE_SENSITIVE_ORDER = new Comparator<FieldKey>()
    {
        @Override
        public int compare(FieldKey a, FieldKey b)
        {
            if (a == b) return 0;
            if (null == a) return -1;
            if (null == b) return 1;
            int c = compare(a.getParent(), b.getParent());
            return c != 0 ? c : a.getName().compareTo(b.getName());
        }
    };


    public static final class Converter implements org.apache.commons.beanutils.Converter
    {
        @Override
        public Object convert(Class type, Object value)
        {
            if (value == null)
                return null;

            if (value instanceof FieldKey)
                return value;

            if (value instanceof String)
                return FieldKey.fromString((String) value);
            else if (value instanceof String[])
                return FieldKey.fromParts((String[]) value);
            else if (value instanceof FieldKey[])
                return FieldKey.fromParts((FieldKey[]) value);
            else if (value instanceof List)
                // XXX: convert items in List?
                return FieldKey.fromParts((List) value);

            throw new ConversionException("Could not convert '" + value + "' to a FieldKey");
        }
    }
}

