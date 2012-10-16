package org.codehaus.cargo.deployer.eba;

import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.deployable.EBA;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.ContainerCapabilityFactory;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationCapabilityFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.generic.deployer.DeployerFactory;
import org.codehaus.cargo.generic.packager.PackagerFactory;

public class AriesFactoryRegistry extends AbstractFactoryRegistry
{

    public static final String EMBEDDED_CONTAINER_ID = "osgi";

    public AriesFactoryRegistry()
    {
        super();
    }

    @Override
    protected void register(DeployableFactory deployableFactory)
    {
        deployableFactory.registerDeployable(EMBEDDED_CONTAINER_ID, EBA.TYPE, EBA.class);
    }

    @Override
    protected void register(ConfigurationCapabilityFactory configurationCapabilityFactory)
    {
        return;
    }

    @Override
    protected void register(ConfigurationFactory configurationFactory)
    {
        return;
    }

    @Override
    protected void register(DeployerFactory deployerFactory)
    {
        deployerFactory.registerDeployer(EMBEDDED_CONTAINER_ID, DeployerType.EMBEDDED,
            AriesEmbeddedLocalDeployer.class);
    }

    @Override
    protected void register(PackagerFactory packagerFactory)
    {
        return;
    }

    @Override
    protected void register(ContainerFactory containerFactory)
    {
        return;
    }

    @Override
    protected void register(ContainerCapabilityFactory containerCapabilityFactory)
    {
        containerCapabilityFactory.registerContainerCapability(EMBEDDED_CONTAINER_ID,
            AriesEmbeddedContainerCapability.class);
    }

}
