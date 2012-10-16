package org.codehaus.cargo.deployable;

import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.spi.deployable.AbstractDeployable;

public class EBA extends AbstractDeployable
{

    public static final DeployableType TYPE = DeployableType.toType("eba");

    public EBA(String file)
    {
        super(file);
    }

    public DeployableType getType()
    {
        return DeployableType.toType("eba");
    }

}
