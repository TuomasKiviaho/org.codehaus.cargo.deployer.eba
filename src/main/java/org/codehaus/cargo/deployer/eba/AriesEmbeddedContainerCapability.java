package org.codehaus.cargo.deployer.eba;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.deployable.EBA;

public class AriesEmbeddedContainerCapability implements ContainerCapability
{

    public static final ContainerCapability INSTANCE = new AriesEmbeddedContainerCapability();

    private Set<DeployableType> deployableTypes;

    public AriesEmbeddedContainerCapability()
    {
        this.deployableTypes = new HashSet<DeployableType>(Arrays.asList(EBA.TYPE));
    }

    public boolean supportsDeployableType(DeployableType deployableType)
    {
        return this.deployableTypes.contains(deployableType);
    }

}
