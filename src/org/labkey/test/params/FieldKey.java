package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldKey
{
    public static final FieldKey ROOT = new FieldKey(null, null);

    private final FieldKey _parent;
    private final String _name;
    private final String _encodedName;

    private FieldKey(FieldKey parent, String name)
    {
        _parent = parent;
        _name = name;
        _encodedName = encodePart(name);
    }

    public static FieldKey fromParts(List<String> parts)
    {
        if (parts.isEmpty())
            return ROOT;

        if (parts.stream().anyMatch(StringUtils::isBlank))
            throw new IllegalArgumentException("parts contains blank: " + parts);

        FieldKey parent = FieldKey.fromParts(parts.subList(0, parts.size() - 1));
        return new FieldKey(parent, parts.get(parts.size() - 1));
    }

    public static FieldKey fromParts(String... parts)
    {
        return fromParts(Arrays.asList(parts));
    }

    public static FieldKey fromPath(String path)
    {
        return fromParts(path.split("/"));
    }

    public static FieldKey fromFieldKey(String path)
    {
        return fromParts(Arrays.stream(path.split("/")).map(FieldKey::decodePart).toList());
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

    public FieldKey child(String fieldName)
    {
        return new FieldKey(_parent, fieldName);
    }

    public List<String> getParts(boolean encode)
    {
        if (this == ROOT)
        {
            return new ArrayList<>();
        }
        else
        {
            List<String> parts = _parent.getParts(encode);
            parts.add(encode ? _encodedName : _name);
            return parts;
        }
    }

    public List<FieldKey> getHierarchy()
    {
        if (this == ROOT)
        {
            return new ArrayList<>();
        }
        else
        {
            List<FieldKey> parts = _parent.getHierarchy();
            parts.add(this);
            return parts;
        }
    }

    @Override
    public String toString()
    {
        return String.join("/", getParts(true));
    }
}
