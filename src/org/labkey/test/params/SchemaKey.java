package org.labkey.test.params;

import java.util.Arrays;
import java.util.List;

public class SchemaKey extends QueryKey<SchemaKey>
{

    private SchemaKey(SchemaKey parent, String child)
    {
        super(parent, child);
    }

    public static SchemaKey fromParts(List<String> parts)
    {
        return QueryKey.fromParts(SchemaKey::new, parts);
    }

    public static SchemaKey fromParts(String... parts)
    {
        return fromParts(Arrays.asList(parts));
    }

    public static SchemaKey parse(String schemaKey)
    {
        return fromParts(Arrays.stream(schemaKey.split("\\.")).map(QueryKey::decodePart).toList());
    }

    @Override
    protected String getDivider()
    {
        return ".";
    }

    @Override
    protected SchemaKey getThis()
    {
        return this;
    }

}
