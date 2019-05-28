package org.labkey.serverapi.reader;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class AbstractDataLoaderFactory extends AbstractDocumentParser implements DataLoaderFactory
{
    public AbstractDataLoaderFactory()
    {
        super();
    }

    @NotNull
    public DataLoader createLoader(InputStream is, boolean hasColumnHeaders) throws IOException
    {
        return createLoader(is, hasColumnHeaders);
    }

    @NotNull
    public DataLoader createLoader(File file, boolean hasColumnHeaders) throws IOException
    {
        return createLoader(file, hasColumnHeaders);
    }

    @Override
    public void parseContent(InputStream stream, ContentHandler h) throws IOException, SAXException
    {
        DataLoader loader = createLoader(stream, true);
        ColumnDescriptor[] cols = loader.getColumns();


        startTag(h, "pre");
        newline(h);

        for (ColumnDescriptor cd : cols)
        {
            if (!cd.load)
                continue;

            write(h, cd.name);
            tab(h);
        }
        newline(h);

        for (Map<String, Object> row : loader)
        {
            for (ColumnDescriptor cd : cols)
            {
                if (!cd.load)
                    continue;

                Object value = row.get(cd.name);
                if (value != null)
                {
                    if (value instanceof String)
                    {
                        String str = (String)value;
                        write(h, str);
                    }
                    else
                        write(h, String.valueOf(value));
                }
                tab(h);
            }

            newline(h);
        }
        endTag(h, "pre");
    }

}