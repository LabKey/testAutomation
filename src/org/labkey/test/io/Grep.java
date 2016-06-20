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
