/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil
{
    private List<File> fileList;
    private final File _dest;
    private final File _source;

    public ZipUtil(File source, File destDir)
    {
        fileList = new ArrayList<>();
        _source = source;
        _dest = new File(destDir, source.getName() + ".zip");

    }

    public File zipIt() throws IOException
    {
        generateFileList(_source);

        System.out.println("Output to Zip : " + _dest.toString());
        _dest.getParentFile().mkdirs();
        _dest.createNewFile();

        int fileCount = 0;
        byte[] buffer = new byte[1024];

        try(
                FileOutputStream fos = new FileOutputStream(_dest);
                ZipOutputStream zos = new ZipOutputStream(fos)
        )
        {
            for (File file : this.fileList)
            {
                ZipEntry ze = generateZipEntry(file);
                zos.putNextEntry(ze);
                try (FileInputStream in = new FileInputStream(file))
                {

                    int len;
                    while ((len = in.read(buffer)) > 0)
                    {
                        zos.write(buffer, 0, len);
                    }
                }
                fileCount++;
            }

            zos.closeEntry();

            System.out.println(String.format("Zipped %d files", fileCount));
        }

        return _dest;
    }

    private void generateFileList(File node)
    {
        // add file only
        if (node.isFile())
        {
            fileList.add(node);
        }

        if (node.isDirectory())
        {
            String[] subNote = node.list();
            for (String filename : subNote)
            {
                generateFileList(new File(node, filename));
            }
        }
    }

    private ZipEntry generateZipEntry(File file)
    {
        if (_source.equals(file)) // zipping a single file
            return new ZipEntry(file.getName());
        else
            return new ZipEntry(_source.toURI().relativize(file.toURI()).getPath());
    }
}