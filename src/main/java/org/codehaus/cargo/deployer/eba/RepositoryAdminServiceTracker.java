package org.codehaus.cargo.deployer.eba;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.osgi.ServiceTracker;
import org.codehaus.cargo.util.CargoException;
import org.codehaus.cargo.util.log.Logger;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class RepositoryAdminServiceTracker extends
    ServiceTracker<RepositoryAdmin, RepositoryAdmin>
{

    private static final String REPOSITORY_NAME = "repository.xml";

    private List<URL> repositories;

    private Logger logger;

    public RepositoryAdminServiceTracker(BundleContext bundleContext,
        final Configuration configuration)
    {
        super(bundleContext, RepositoryAdmin.class, null);
        PropertyResolver propertyResolver = new PropertyResolver()
        {

            public String get(String propertyName)
            {
                return configuration.getPropertyValue(propertyName);
            }

        };
        MavenConfigurationImpl mavenConfiguration =
            new MavenConfigurationImpl(propertyResolver, this.getClass().getPackage().getName());
        URL settingsFileUrl = mavenConfiguration.getSettingsFileUrl();
        Boolean useFallbackRepositories = mavenConfiguration.useFallbackRepositories();
        MavenSettingsImpl mavenSettings =
            useFallbackRepositories == null ? new MavenSettingsImpl(settingsFileUrl)
                : new MavenSettingsImpl(settingsFileUrl, useFallbackRepositories);
        mavenConfiguration.setSettings(mavenSettings);
        List<MavenRepositoryURL> mavenRepositoryURLs;
        try
        {
            mavenRepositoryURLs = mavenConfiguration.getRepositories();
        }
        catch (MalformedURLException e)
        {
            throw new CargoException(e.getMessage(), e);
        }
        MavenRepositoryURL localRepository = mavenConfiguration.getLocalRepository();
        if (localRepository != null)
        {
            List<MavenRepositoryURL> repositories = mavenRepositoryURLs;
            mavenRepositoryURLs = new ArrayList<MavenRepositoryURL>(repositories.size() + 1);
            mavenRepositoryURLs.add(localRepository);
            mavenRepositoryURLs.addAll(repositories);
        }
        this.repositories = new ArrayList<URL>(mavenRepositoryURLs.size());
        for (MavenRepositoryURL mavenRepositoryURL : mavenRepositoryURLs)
        {
            File repositoryFile = mavenRepositoryURL.getFile();
            URL repository;
            if (repositoryFile == null)
            {
                URL url = mavenRepositoryURL.getURL();
                try
                {
                    repository = new URL(url, REPOSITORY_NAME);
                }
                catch (MalformedURLException e)
                {
                    throw new CargoException(url.toExternalForm(), e);
                }
            }
            else
            {
                File file = new File(repositoryFile, REPOSITORY_NAME);
                try
                {
                    repository = file.toURI().toURL();
                }
                catch (MalformedURLException e)
                {
                    throw new CargoException(file.toString(), e);
                }
            }
            this.repositories.add(repository);
        }
        this.logger = configuration.getLogger();
    }

    void execute(RepositoryAdmin repositoryAdmin)
    {
        for (URL repository : this.repositories)
        {
            try
            {
                repositoryAdmin.addRepository(repository);
            }
            catch (Exception e)
            {
                this.logger.warn(repositoryAdmin.getClass().getName(),
                    repository.toExternalForm());
            }
        }
    }

    public RepositoryAdmin addingService(ServiceReference<RepositoryAdmin> serviceReference)
    {
        RepositoryAdmin repositoryAdmin = (RepositoryAdmin) super.addingService(serviceReference);
        execute(repositoryAdmin);
        return repositoryAdmin;
    }

    public void removedService(ServiceReference<RepositoryAdmin> serviceReference,
        RepositoryAdmin repositoryAdmin)
    {
        super.removedService(serviceReference, repositoryAdmin);
        for (URL repository : this.repositories)
        {
            repositoryAdmin.removeRepository(repository.toExternalForm());
        }
    }
}
