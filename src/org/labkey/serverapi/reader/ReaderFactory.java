package org.labkey.serverapi.reader;

import java.io.BufferedReader;
import java.io.IOException;

public interface ReaderFactory
{
    BufferedReader getReader() throws IOException;
}
