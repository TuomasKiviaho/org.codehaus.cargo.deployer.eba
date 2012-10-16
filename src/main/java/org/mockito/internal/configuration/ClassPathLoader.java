/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Scanner;

import org.mockito.configuration.IMockitoConfiguration;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.exceptions.misusing.MockitoConfigurationException;
import org.mockito.internal.creation.CglibMockMaker;
import org.mockito.plugins.MockMaker;

public class ClassPathLoader
{
    private static final MockMaker mockMaker = findPlatformMockMaker();

    public static final String MOCKITO_CONFIGURATION_CLASS_NAME =
        "org.mockito.configuration.MockitoConfiguration";

    public IMockitoConfiguration loadConfiguration()
    {
        IMockitoConfiguration mockitoConfiguration;
        try
        {
            ClassLoader classLoader = ClassPathLoader.class.getClassLoader();
            Class< ? > configClass = classLoader.loadClass(MOCKITO_CONFIGURATION_CLASS_NAME);
            mockitoConfiguration = (IMockitoConfiguration) configClass.newInstance();
        }
        catch (ClassNotFoundException e)
        {
            mockitoConfiguration = null;
        }
        catch (ClassCastException e)
        {
            throw new MockitoConfigurationException("MockitoConfiguration class must implement "
                + IMockitoConfiguration.class.getName() + " interface.", e);
        }
        catch (Exception e)
        {
            throw new MockitoConfigurationException("Unable to instantiate "
                + MOCKITO_CONFIGURATION_CLASS_NAME
                + " class. Does it have a safe, no-arg constructor?", e);
        }
        return mockitoConfiguration;
    }

    public static MockMaker getMockMaker()
    {
        return mockMaker;
    }

    static MockMaker findPlatformMockMaker()
    {
        ClassLoader classLoader = ClassPathLoader.class.getClassLoader();
        Enumeration<URL> resources;
        try
        {
            resources =
                classLoader.getResources("mockito-extensions/" + MockMaker.class.getName());
        }
        catch (IOException e)
        {
            throw new MockitoException("Failed to load " + MockMaker.class.getName(), e);
        }
        MockMaker mockMaker = new CglibMockMaker();
        while (resources.hasMoreElements())
        {
            URL resource = resources.nextElement();
            try
            {
                InputStream inputStream = resource.openStream();
                try
                {
                    for (Scanner scanner = new Scanner(inputStream, "UTF-8"); scanner
                        .hasNextLine();)
                    {
                        String mockMakerClassName = scanner.nextLine();
                        int index = mockMakerClassName.indexOf('#');
                        if (!(index < 0))
                        {
                            mockMakerClassName = mockMakerClassName.substring(0, index);
                        }
                        mockMakerClassName = mockMakerClassName.trim();
                        if (!mockMakerClassName.isEmpty())
                        {
                            Class< ? extends MockMaker> mockMakerClass =
                                classLoader.loadClass(mockMakerClassName).asSubclass(
                                    MockMaker.class);
                            mockMaker = mockMakerClass.newInstance();
                            break;
                        }
                    }
                }
                finally
                {
                    inputStream.close();
                }
            }
            catch (Exception e)
            {
                throw new MockitoConfigurationException("Failed to load "
                    + MockMaker.class.getName() + " using " + resource, e);
            }
        }
        return mockMaker;
    }

}
