/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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