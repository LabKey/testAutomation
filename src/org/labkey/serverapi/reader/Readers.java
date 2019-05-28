package org.labkey.serverapi.reader;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.XmlStreamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Readers
{
    public static Reader getUnbufferedReader(InputStream in)
    {
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    public static Reader getUnbufferedReader(File file) throws FileNotFoundException
    {
        return getUnbufferedReader(new FileInputStream(file));
    }

    public static BufferedReader getReader(InputStream in)
    {
        return new BufferedReader(getUnbufferedReader(in));
    }

    public static BufferedReader getReader(File file) throws FileNotFoundException
    {
        return getReader(new FileInputStream(file));
    }

    // Detects XML file character encoding based on BOM, XML prolog, or content type... falling back on UTF-8
    public static BufferedReader getXmlReader(InputStream in) throws IOException
    {
        return new BufferedReader(new XmlStreamReader(in));
    }

    // Detects XML file character encoding based on BOM, XML prolog, or content type... falling back on UTF-8
    public static BufferedReader getXmlReader(File file) throws IOException
    {
        return new BufferedReader(new XmlStreamReader(file));
    }

    /**
     * Detects text file character encoding based on BOM... falling back on UTF-8 if no BOM present
     */
    public static BufferedReader getBOMDetectingReader(InputStream in) throws IOException
    {
        BOMInputStream bos = new BOMInputStream(in, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);
        Charset charset = bos.hasBOM() ? Charset.forName(bos.getBOM().getCharsetName()) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(bos, charset));
    }

    /**
     * Detects text file character encoding based on BOM... falling back on UTF-8 if no BOM present
     */
    public static BufferedReader getBOMDetectingReader(File file) throws IOException
    {
        return getBOMDetectingReader(new FileInputStream(file));
    }
}

