package org.apache.aries.application.resolver.obr.ext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.modelling.Consumer;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.impl.ImportedServiceImpl;
import org.apache.aries.application.resolver.obr.ext.BundleResource;
import org.apache.aries.application.resolver.obr.ext.BundleResourceTransformer;
import org.apache.aries.application.resolver.obr.impl.RequirementImpl;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;

public class ImportedServiceTransformer implements BundleResourceTransformer
{

    private static Field consumerField;

    private static Field interfaceField;

    private static Field attributesField;

    private static Method generateAttributeFilterMethod;

    private static Field attributeFilterField;

    static
    {
        try
        {
            consumerField = RequirementImpl.class.getDeclaredField("consumer");
            consumerField.setAccessible(true);

            interfaceField = ImportedServiceImpl.class.getDeclaredField("_iface");
            interfaceField.setAccessible(true);

            attributesField = ImportedServiceImpl.class.getDeclaredField("_attributes");
            attributesField.setAccessible(true);

            generateAttributeFilterMethod =
                ImportedServiceImpl.class.getDeclaredMethod("generateAttributeFilter", Map.class);
            generateAttributeFilterMethod.setAccessible(true);

            attributeFilterField = ImportedServiceImpl.class.getDeclaredField("_attributeFilter");
            attributeFilterField.setAccessible(true);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    public ImportedServiceTransformer()
    {
        super();
    }

    public BundleResource transform(BundleResource bundleResource)
    {
        BundleResource transformedBundleResource = bundleResource;
        Requirement[] requirements = transformedBundleResource.getRequirements();
        for (Requirement requirement : requirements)
        {
            String name = requirement.getName();
            if (ModellingConstants.OBR_SERVICE.equals(name)
                && requirement instanceof RequirementImpl)
            {
                try
                {
                    Consumer consumer = (Consumer) consumerField.get(requirement);
                    if (consumer instanceof ImportedServiceImpl)
                    {
                        String serviceInterface = (String) interfaceField.get(consumer);

                        @SuppressWarnings("unchecked")
                        Map<String, String> _attributes =
                            (Map<String, String>) attributesField.get(consumer);
                        _attributes.put(ModellingConstants.OBR_SERVICE, serviceInterface);
                        _attributes.remove(Constants.OBJECTCLASS);

                        Filter filter =
                            (Filter) generateAttributeFilterMethod.invoke(consumer,
                                new HashMap<String, String>());

                        attributeFilterField.set(consumer, filter);
                    }
                }
                catch (Exception e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
        return transformedBundleResource;
    }

}
