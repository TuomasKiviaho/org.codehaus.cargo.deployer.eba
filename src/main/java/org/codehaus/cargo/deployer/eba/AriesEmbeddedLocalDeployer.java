package org.codehaus.cargo.deployer.eba;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.spi.convert.BundleConverter;
import org.apache.aries.application.management.spi.repository.PlatformRepository;
import org.apache.aries.application.management.spi.resolve.AriesApplicationResolver;
import org.apache.aries.application.management.spi.resolve.PostResolveTransformer;
import org.apache.aries.application.management.spi.resolve.PreResolveHook;
import org.apache.aries.application.management.spi.runtime.LocalPlatform;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ServiceModeller;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.osgi.OsgiEmbeddedLocalDeployer;
import org.codehaus.cargo.container.osgi.Proxy;
import org.codehaus.cargo.container.osgi.ServiceTracker;
import org.codehaus.cargo.util.log.Loggable;
import org.codehaus.cargo.util.log.Logger;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class AriesEmbeddedLocalDeployer<T extends EmbeddedLocalContainer & BundleReference>
    extends OsgiEmbeddedLocalDeployer<T>
{

    private static class DeployableComparator implements Comparator<Deployable>
    {

        public DeployableComparator()
        {
            super();
        }

        public int compare(Deployable deployable1, Deployable deployable2)
        {
            String file1 = deployable1.getFile();
            String file2 = deployable2.getFile();
            return file1.compareTo(file2);
        }

    }

    private class ApplicationManagerServiceTracker extends
        ServiceTracker<AriesApplicationManager, AriesApplicationManager>
    {

        public ApplicationManagerServiceTracker(BundleContext bundleContext)
        {
            super(bundleContext, AriesApplicationManager.class, null);
        }

        @Override
        public AriesApplicationManager addingService(
            ServiceReference<AriesApplicationManager> serviceReference)
        {
            AriesApplicationManager applicationManager = super.addingService(serviceReference);
            synchronized (AriesEmbeddedLocalDeployer.this.applicationManagers)
            {
                Comparator<Deployable> comparator = AriesEmbeddedLocalDeployer.this.comparator;
                Map<Deployable, AriesApplicationContext> deployables =
                    new ConcurrentSkipListMap<Deployable, AriesApplicationContext>(comparator);
                AriesEmbeddedLocalDeployer.this.applicationManagers.put(applicationManager,
                    deployables);
                Set<Entry<Deployable, Boolean>> entrySet =
                    AriesEmbeddedLocalDeployer.this.deployables.entrySet();
                for (Map.Entry<Deployable, Boolean> entry : entrySet)
                {
                    Deployable deployable = entry.getKey();
                    AriesApplicationContext applicationContext =
                        install(applicationManager, deployable);
                    deployables.put(deployable, applicationContext);
                    Boolean started = entry.getValue();
                    if (started != null && started.booleanValue())
                    {
                        start(deployable, applicationContext);
                    }
                }
            }
            return applicationManager;
        }

        @Override
        public void modifiedService(ServiceReference<AriesApplicationManager> serviceReference,
            AriesApplicationManager applicationManager)
        {
            synchronized (AriesEmbeddedLocalDeployer.this.applicationManagers)
            {
                Map<Deployable, AriesApplicationContext> deployables =
                    AriesEmbeddedLocalDeployer.this.applicationManagers
                        .remove(applicationManager);
                for (Iterator<Map.Entry<Deployable, AriesApplicationContext>> iterator =
                    deployables.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry<Deployable, AriesApplicationContext> entry = iterator.next();
                    Deployable deployable = entry.getKey();
                    AriesApplicationContext applicationContext =
                        update(applicationManager, deployable);
                    entry.setValue(applicationContext);
                }
            }
            super.modifiedService(serviceReference, applicationManager);
        }

        @Override
        public void removedService(ServiceReference<AriesApplicationManager> serviceReference,
            AriesApplicationManager applicationManager)
        {
            synchronized (AriesEmbeddedLocalDeployer.this.applicationManagers)
            {
                Map<Deployable, AriesApplicationContext> deployables =
                    AriesEmbeddedLocalDeployer.this.applicationManagers
                        .remove(applicationManager);
                for (Iterator<Map.Entry<Deployable, AriesApplicationContext>> iterator =
                    deployables.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry<Deployable, AriesApplicationContext> entry = iterator.next();
                    iterator.remove();
                    Deployable deployable = entry.getKey();
                    AriesApplicationContext applicationContext = entry.getValue();
                    uninstall(applicationManager, deployable, applicationContext);
                }
            }
            super.removedService(serviceReference, applicationManager);
        }

    }

    private static Map<EmbeddedLocalContainer, AriesEmbeddedLocalDeployer< ? >> embeddedLocalDeployers =
        new WeakHashMap<EmbeddedLocalContainer, AriesEmbeddedLocalDeployer< ? >>();

    private Map<Deployable, Boolean> deployables;

    private Map<AriesApplicationManager, Map<Deployable, AriesApplicationContext>> applicationManagers;

    private Comparator<Deployable> comparator;

    private static Loggable getLoggableLoggerFactory()
    {
        org.slf4j.impl.StaticLoggerBinder staticLoggerBinder =
            org.slf4j.impl.StaticLoggerBinder.getSingleton();
        org.slf4j.ILoggerFactory loggerFactory = staticLoggerBinder.getLoggerFactory();
        Loggable loggable = loggerFactory instanceof Loggable ? (Loggable) loggerFactory : null;
        return loggable;
    }

    private static <T extends EmbeddedLocalContainer & BundleReference> AriesApplicationManager createApplicationManager(
        T embeddedLocalContainer)
    {
        Bundle bundle = embeddedLocalContainer.getBundle();
        BundleContext bundleContext = bundle.getBundleContext();
        Configuration configuration = embeddedLocalContainer.getConfiguration();
        org.codehaus.cargo.deployer.eba.RepositoryAdminServiceTracker repositoryAdminServiceTracker =
            new org.codehaus.cargo.deployer.eba.RepositoryAdminServiceTracker(bundleContext,
                configuration);
        try
        {
            // TODO build app. manager using Aries Blueprint via mock BundleContext
            org.apache.aries.application.management.impl.AriesApplicationManagerImpl applicationManager =
                new org.apache.aries.application.management.impl.AriesApplicationManagerImpl();
            applicationManager.setBundleContext(bundleContext);

            LocalPlatform localPlatform =
                new org.apache.aries.application.local.platform.impl.DefaultLocalPlatform();
            applicationManager.setLocalPlatform(localPlatform);

            ModellingManager modellingManager =
                new org.apache.aries.application.modelling.impl.ModellingManagerImpl();

            ModellingHelper modellingHelper =
                new org.apache.aries.application.modelling.utils.impl.ModellingHelperImpl();

            org.apache.felix.utils.log.Logger felixLogger =
                new org.apache.felix.utils.log.Logger(bundleContext);
            org.apache.felix.bundlerepository.RepositoryAdmin repositoryAdmin =
                new org.apache.felix.bundlerepository.impl.RepositoryAdminImpl(bundleContext,
                    felixLogger);
            repositoryAdminServiceTracker.execute(repositoryAdmin);

            final org.apache.aries.application.resolver.obr.OBRAriesResolver applicationResolver =
                new org.apache.aries.application.resolver.obr.OBRAriesResolver(repositoryAdmin);
            PlatformRepository platformRepository = null;
            applicationResolver.setPlatformRepository(platformRepository);
            applicationResolver.setModellingManager(modellingManager);
            applicationResolver.setModellingHelper(modellingHelper);

            {
                org.apache.aries.application.deployment.management.impl.DeploymentManifestManagerImpl deploymentManifestManager =
                    new org.apache.aries.application.deployment.management.impl.DeploymentManifestManagerImpl();
                deploymentManifestManager.setLocalPlatform(localPlatform);
                deploymentManifestManager.setResolver(applicationResolver);
                deploymentManifestManager.setModellingManager(modellingManager);
                deploymentManifestManager.setModellingHelper(modellingHelper);

                org.apache.aries.application.modelling.impl.ModelledResourceManagerImpl modelledResourceManager =
                    new org.apache.aries.application.modelling.impl.ModelledResourceManagerImpl();
                modelledResourceManager.setModellingManager(modellingManager);

                Collection<ServiceModeller> serviceModellers = Collections.emptyList();
                modelledResourceManager.setModellingPlugins(serviceModellers);
                {
                    org.apache.aries.application.modelling.impl.ParserProxyImpl parserProxy =
                        new org.apache.aries.application.modelling.impl.ParserProxyImpl();
                    org.apache.aries.blueprint.services.ParserService parserService = null;
                    parserProxy.setParserService(parserService);
                    parserProxy.setBundleContext(bundleContext);
                    parserProxy.setModellingManager(modellingManager);
                    modelledResourceManager.setParserProxy(parserProxy);
                }
                deploymentManifestManager.setModelledResourceManager(modelledResourceManager);

                org.apache.aries.application.resolver.obr.impl.RepositoryGeneratorImpl repositoryGenerator =
                    new org.apache.aries.application.resolver.obr.impl.RepositoryGeneratorImpl(repositoryAdmin);
                repositoryGenerator.setModelledResourceManager(modelledResourceManager);
                repositoryGenerator.setTempDir(localPlatform);

                org.apache.aries.application.resolver.obr.ext.BundleResourceTransformer bundleResourceTransformer =
                    new org.apache.aries.application.resolver.obr.ext.ImportedServiceTransformer();
                repositoryGenerator.setBundleResourceTransformers(Collections
                    .singletonList(bundleResourceTransformer));

                PostResolveTransformer postResolveTransformer = null;
                deploymentManifestManager.setPostResolveTransformer(postResolveTransformer);

                List<PreResolveHook> preResolveHooks = Collections.emptyList();
                deploymentManifestManager.setPreResolveHooks(preResolveHooks);

                applicationManager.setDeploymentManifestManager(deploymentManifestManager);
            }
            ApplicationMetadataFactory applicationMetadataFactory =
                new org.apache.aries.application.impl.ApplicationMetadataFactoryImpl();
            applicationManager.setApplicationMetadataFactory(applicationMetadataFactory);

            DeploymentMetadataFactory deploymentMetadataFactory =
                new org.apache.aries.application.impl.DeploymentMetadataFactoryImpl();
            applicationManager.setDeploymentMetadataFactory(deploymentMetadataFactory);

            List<BundleConverter> bundleConverters = Collections.emptyList();
            applicationManager.setBundleConverters(bundleConverters);

            org.apache.aries.application.runtime.impl.ApplicationContextManagerImpl applicationContextManager =
                new org.apache.aries.application.runtime.impl.ApplicationContextManagerImpl();

            BundleContext spy = Mockito.spy(bundleContext);
            Answer<ServiceReference< ? >> applicationResolverServiceReferenceAnswer =
                new Answer<ServiceReference< ? >>()
                {

                    public ServiceReference< ? > answer(InvocationOnMock invocation)
                        throws Throwable
                    {
                        ServiceReference< ? > serviceReference =
                            (ServiceReference< ? >) invocation.callRealMethod();
                        if (serviceReference == null)
                        {
                            serviceReference = Mockito.mock(ServiceReference.class);
                        }
                        return serviceReference;
                    }

                };
            Mockito.doAnswer(applicationResolverServiceReferenceAnswer).when(spy)
                .getServiceReference(AriesApplicationResolver.class.getName());
            Answer< ? > serviceAnswer = new Answer<Object>()
            {

                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    Object service = invocation.callRealMethod();
                    if (service != null && !applicationResolver.equals(service))
                    {
                        Object[] arguments = invocation.getArguments();
                        ServiceReference< ? > serviceReference =
                            (ServiceReference< ? >) arguments[0];
                        String[] classNames =
                            (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
                        ClassLoader classLoader = this.getClass().getClassLoader();
                        for (String className : classNames)
                        {
                            Class< ? > type = classLoader.loadClass(className);
                            service = Proxy.newInstance(type, service);
                            break;
                        }
                    }
                    return service;
                }

            };
            Mockito.doAnswer(serviceAnswer).when(spy)
                .getService((ServiceReference< ? >) Mockito.any(ServiceReference.class));
            Matcher<ServiceReference< ? >> isMock = new ArgumentMatcher<ServiceReference< ? >>()
            {

                public boolean matches(Object argument)
                {
                    return Mockito.mockingDetails(argument).isMock();
                }

            };
            Mockito.doReturn(applicationResolver).when(spy).getService(Mockito.argThat(isMock));
            Mockito.doReturn(Boolean.TRUE).when(spy).ungetService(Mockito.argThat(isMock));
            applicationContextManager.setBundleContext(spy);
            applicationManager.setApplicationContextManager(applicationContextManager);

            return applicationManager;
        }
        finally
        {
            repositoryAdminServiceTracker.open();
        }
    }

    public AriesEmbeddedLocalDeployer(T embeddedLocalContainer)
    {
        super(embeddedLocalContainer);
        Logger logger = embeddedLocalContainer.getLogger();
        try
        {
            Loggable loggable = getLoggableLoggerFactory();
            if (loggable != null)
            {
                loggable.setLogger(logger);
            }
        }
        catch (NoClassDefFoundError e)
        {
            ;
        }
        AriesEmbeddedLocalDeployer< ? > embeddedLocalDeployer =
            AriesEmbeddedLocalDeployer.embeddedLocalDeployers.remove(embeddedLocalContainer);
        if (embeddedLocalDeployer == null)
        {
            this.comparator = new DeployableComparator();
            this.deployables = new TreeMap<Deployable, Boolean>(this.comparator);
            this.applicationManagers =
                new ConcurrentHashMap<AriesApplicationManager, Map<Deployable, AriesApplicationContext>>(1);
            AriesEmbeddedLocalDeployer.embeddedLocalDeployers.put(embeddedLocalContainer, this);
            Bundle bundle = embeddedLocalContainer.getBundle();
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext == null)
            {
                LocalConfiguration configuration = embeddedLocalContainer.getConfiguration();
                configuration.configure(embeddedLocalContainer);
                bundleContext = bundle.getBundleContext();
            }
            ServiceTracker< ? , ? > serviceTracker =
                new ApplicationManagerServiceTracker(bundleContext);
            serviceTracker.open();
            try
            {
                AriesApplicationManager applicationManager =
                    createApplicationManager(embeddedLocalContainer);
                Map<Deployable, AriesApplicationContext> deployables =
                    new ConcurrentSkipListMap<Deployable, AriesApplicationContext>(this.comparator);
                this.applicationManagers.put(applicationManager, deployables);
            }
            catch (NoClassDefFoundError e)
            {
                logger.info(this.getClass().getName(), e.getMessage());
            }
        }
        else
        {
            this.deployables = embeddedLocalDeployer.deployables;
            this.applicationManagers = embeddedLocalDeployer.applicationManagers;
        }
        AriesEmbeddedLocalDeployer.embeddedLocalDeployers.put(embeddedLocalContainer, this);
    }

    private static AriesApplication createApplication(AriesApplicationManager applicationManager,
        Deployable deployable) throws ManagementException
    {
        String deployableFile = deployable.getFile();
        File file = new File(deployableFile);
        IDirectory source = FileSystem.getFSRoot(file);
        AriesApplication application = applicationManager.createApplication(source);
        return application;
    }

    private static AriesApplicationContext install(AriesApplicationManager applicationManager,
        Deployable deployable) throws DeployableException
    {
        AriesApplicationContext applicationContext;
        try
        {
            AriesApplication application = createApplication(applicationManager, deployable);
            applicationContext = applicationManager.install(application);
        }
        catch (Exception e)
        {
            throw new DeployableException(deployable.getFile(), e);
        }
        return applicationContext;
    }

    private static void uninstall(AriesApplicationManager applicationManager,
        Deployable deployable, AriesApplicationContext applicationContext)
        throws DeployableException
    {
        try
        {
            applicationManager.uninstall(applicationContext);
        }
        catch (Exception e)
        {
            throw new DeployableException(deployable.getFile(), e);
        }
    }

    private static AriesApplicationContext update(AriesApplicationManager applicationManager,
        Deployable deployable) throws DeployableException
    {
        AriesApplicationContext applicationContext;
        try
        {
            AriesApplication application = createApplication(applicationManager, deployable);
            DeploymentMetadata deploymentMetadata = application.getDeploymentMetadata();
            applicationContext = applicationManager.update(application, deploymentMetadata);
        }
        catch (Exception e)
        {
            throw new DeployableException(deployable.getFile(), e);
        }
        return applicationContext;
    }

    private static void start(Deployable deployable, AriesApplicationContext applicationContext)
        throws DeployableException
    {
        try
        {
            applicationContext.start();
        }
        catch (Exception e)
        {
            throw new DeployableException(deployable.getFile(), e);
        }
    }

    private static void stop(Deployable deployable, AriesApplicationContext applicationContext)
        throws DeployableException
    {
        try
        {
            applicationContext.stop();
        }
        catch (Exception e)
        {
            throw new DeployableException(deployable.getFile(), e);
        }
    }

    @Override
    public void deploy(Deployable deployable) throws DeployableException
    {
        DeployableType deployableType = deployable.getType();
        if (AriesEmbeddedContainerCapability.INSTANCE.supportsDeployableType(deployableType))
        {
            synchronized (this.applicationManagers)
            {
                if (!this.deployables.containsKey(deployable))
                {
                    this.deployables.put(deployable, null);
                    for (Map.Entry<AriesApplicationManager, Map<Deployable, AriesApplicationContext>> entry : this.applicationManagers
                        .entrySet())
                    {
                        Map<Deployable, AriesApplicationContext> deployables = entry.getValue();
                        if (!deployables.containsKey(deployable))
                        {
                            AriesApplicationManager applicationManager = entry.getKey();
                            AriesApplicationContext applicationContext =
                                install(applicationManager, deployable);
                            deployables.put(deployable, applicationContext);
                        }
                    }
                }
            }
        }
        else
        {
            super.deploy(deployable);
        }
    }

    @Override
    public void redeploy(Deployable deployable) throws DeployableException
    {
        DeployableType deployableType = deployable.getType();
        if (AriesEmbeddedContainerCapability.INSTANCE.supportsDeployableType(deployableType))
        {
            synchronized (this.applicationManagers)
            {
                if (this.deployables.containsKey(deployable))
                {
                    for (Map.Entry<AriesApplicationManager, Map<Deployable, AriesApplicationContext>> entry : this.applicationManagers
                        .entrySet())
                    {
                        Map<Deployable, AriesApplicationContext> deployables = entry.getValue();
                        AriesApplicationManager applicationManager = entry.getKey();
                        AriesApplicationContext applicationContext =
                            update(applicationManager, deployable);
                        deployables.put(deployable, applicationContext);
                    }
                }
            }
        }
        else
        {
            super.redeploy(deployable);
        }
    }

    @Override
    public void start(Deployable deployable) throws DeployableException
    {
        DeployableType deployableType = deployable.getType();
        if (AriesEmbeddedContainerCapability.INSTANCE.supportsDeployableType(deployableType))
        {
            synchronized (this.applicationManagers)
            {
                if (this.deployables.containsKey(deployable))
                {
                    Boolean started = this.deployables.put(deployable, Boolean.TRUE);
                    if (started == null || !started.booleanValue())
                    {
                        for (Map<Deployable, AriesApplicationContext> deployables : this.applicationManagers
                            .values())
                        {
                            AriesApplicationContext applicationContext =
                                deployables.get(deployable);
                            if (applicationContext != null)
                            {
                                start(deployable, applicationContext);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            super.start(deployable);
        }
    }

    @Override
    public void stop(Deployable deployable) throws DeployableException
    {
        DeployableType deployableType = deployable.getType();
        if (AriesEmbeddedContainerCapability.INSTANCE.supportsDeployableType(deployableType))
        {
            synchronized (this.applicationManagers)
            {
                if (this.deployables.containsKey(deployable))
                {
                    Boolean started = this.deployables.put(deployable, Boolean.FALSE);
                    if (started != null && started.booleanValue())
                    {
                        for (Map<Deployable, AriesApplicationContext> deployables : this.applicationManagers
                            .values())
                        {
                            AriesApplicationContext applicationContext =
                                deployables.remove(deployable);
                            if (applicationContext != null)
                            {
                                stop(deployable, applicationContext);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            super.stop(deployable);
        }
    }

    @Override
    public void undeploy(Deployable deployable) throws DeployableException
    {
        DeployableType deployableType = deployable.getType();
        if (AriesEmbeddedContainerCapability.INSTANCE.supportsDeployableType(deployableType))
        {
            synchronized (this.applicationManagers)
            {
                if (this.deployables.remove(deployable))
                {
                    for (Map.Entry<AriesApplicationManager, Map<Deployable, AriesApplicationContext>> entry : this.applicationManagers
                        .entrySet())
                    {
                        Map<Deployable, AriesApplicationContext> deployables = entry.getValue();
                        AriesApplicationContext applicationContext =
                            deployables.remove(deployable);
                        if (applicationContext != null)
                        {
                            AriesApplicationManager applicationManager = entry.getKey();
                            uninstall(applicationManager, deployable, applicationContext);
                        }
                    }
                }
            }
        }
        else
        {
            super.undeploy(deployable);
        }
    }

}
