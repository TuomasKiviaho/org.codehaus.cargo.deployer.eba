/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.bundlerepository.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.aries.plugin.eba.MojoSupport;
import org.osgi.framework.Constants;

public class FileHeaders implements DataModelHelperImpl.Headers
{
    private Attributes mainAttributes;

    private Properties localization;

    public FileHeaders(File file) throws IOException
    {
        Manifest manifest = MojoSupport.getManifest(file);
        if (manifest == null)
        {
            File manifestFile = new File(file, JarFile.MANIFEST_NAME);
            URI uri = manifestFile.toURI();
            throw new FileNotFoundException(uri.toString());
        }
        this.mainAttributes = manifest.getMainAttributes();
        String bundleLocalization = mainAttributes.getValue(Constants.BUNDLE_LOCALIZATION);
        if (bundleLocalization == null)
        {
            bundleLocalization = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
        }
        bundleLocalization += ".properties";
        InputStream inputStream = null;
        if (file.isDirectory())
        {
            File bundleLocalizationFile = new File(file, bundleLocalization);
            if (bundleLocalizationFile.exists())
            {
                inputStream = new FileInputStream(bundleLocalizationFile);
            }
        }
        else
        {
            JarFile jarFile = new JarFile(file);
            ZipEntry entry = jarFile.getEntry(bundleLocalization);
            if (entry != null)
            {
                inputStream = jarFile.getInputStream(entry);   
            }
        }
        this.localization = new Properties();
        if (inputStream != null)
        {
            try
            {
                this.localization.load(inputStream);
            }
            finally
            {
                inputStream.close();
            }
        }
    }

    public String getHeader(String name)
    {
        String value = this.mainAttributes.getValue(name);
        if (value != null && value.startsWith("%"))
        {
            value = value.substring(1);
            value = this.localization.getProperty(value, value);
        }
        return value;
    }

}
