/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.io;

import org.labkey.api.reader.Readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grep
{
    /**
     * Search files for some specified text
     * @param literalText The text to search for. Must not be multi-line
     * @param files Files to search in
     * @return Collection of files that were found to contain the specified text and the line number where it was found
     */
    public static Map<File, Integer> grep(String literalText, File... files) throws IOException
    {
        if (literalText.contains("\n"))
            throw new IllegalArgumentException("Can only find single lines of text");

        Pattern pattern = Pattern.compile(Pattern.quote(literalText));
        Map<File, Integer> filesContainingSpecifiedText = new HashMap<>();
        for (File file : files)
        {
            int lineNumber = grep(file, pattern);
            if (lineNumber > 0)
                filesContainingSpecifiedText.put(file, lineNumber);
        }
        return filesContainingSpecifiedText;
    }

    private static int grep(File file, Pattern pattern) throws IOException
    {
        try (BufferedReader is = Readers.getReader(file))
        {
            Matcher matcher = pattern.matcher("");
            String line;
            int lineNumber = 0;
            while (null != (line = is.readLine()))
            {
                lineNumber++;
                matcher.reset(line);

                if (matcher.find())
                    return lineNumber;
            }
        }
        return -1;
    }
}
