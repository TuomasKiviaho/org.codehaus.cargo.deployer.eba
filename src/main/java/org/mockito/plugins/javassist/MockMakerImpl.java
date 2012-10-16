package org.mockito.plugins.javassist;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.objenesis.ObjenesisHelper;
import org.objenesis.instantiator.ObjectInstantiator;

public class MockMakerImpl implements MockMaker
{

    private Map<Proxy, MethodHandlerImpl> methodHandlers;

    public MockMakerImpl()
    {
        this.methodHandlers =
            Collections.synchronizedMap(new WeakHashMap<Proxy, MethodHandlerImpl>());
    }

    public <T> T createMock(MockCreationSettings<T> mockCreationSettings, MockHandler mockHandler)
    {
        Class< ? > type = mockCreationSettings.getTypeToMock();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Deque<Class< ? >> interfaces =
            new LinkedList<Class< ? >>((Set) mockCreationSettings.getExtraInterfaces());
        while (Proxy.class.isAssignableFrom(type))
        {
            Class< ? >[] typeInterfaces = type.getInterfaces();
            for (Class< ? > typeInterface : typeInterfaces)
            {
                if (!Proxy.class.isAssignableFrom(typeInterface))
                {
                    interfaces.add(typeInterface);
                }
            }
            type = type.getSuperclass();
        }
        ProxyFactory proxyFactory = new ProxyFactory();
        if (type.isInterface())
        {
            interfaces.addFirst(type);
        }
        else
        {
            proxyFactory.setSuperclass(type);
        }
        proxyFactory.setInterfaces(interfaces.toArray(new Class< ? >[interfaces.size()]));
        Class< ? > proxyClass;
        try
        {
            proxyClass = proxyFactory.createClass();
        }
        catch (RuntimeException e)
        {
            throw new AssertionError(e);
        }
        catch (NoClassDefFoundError e)
        {
            throw new AssertionError(e);
        }
        ObjectInstantiator objectInstantiator =
            mockCreationSettings.isSerializable() ? ObjenesisHelper
                .getSerializableObjectInstantiatorOf(proxyClass) : ObjenesisHelper
                .getInstantiatorOf(proxyClass);
        Proxy proxy = (Proxy) objectInstantiator.newInstance();
        MethodHandlerImpl methodHandler = new MethodHandlerImpl();
        proxy.setHandler(methodHandler);
        if (!(proxy instanceof ProxyObject))
        {
            methodHandlers.put(proxy, methodHandler);
        }
        @SuppressWarnings("unchecked")
        T mock = (T) proxy;
        this.resetMock(mock, mockHandler, mockCreationSettings);
        return mock;
    }

    public MockHandler getHandler(Object mock)
    {
        MethodHandlerImpl methodHandlerImpl;
        if (mock instanceof ProxyObject)
        {
            ProxyObject proxyObject = (ProxyObject) mock;
            MethodHandler methodHandler = (MethodHandler) proxyObject.getHandler();
            methodHandlerImpl =
                methodHandler instanceof MethodHandlerImpl ? (MethodHandlerImpl) methodHandler
                    : null;
        }
        else
        {
            methodHandlerImpl = methodHandlers.get(mock);
        }
        MockHandler mockHandler =
            methodHandlerImpl == null ? null : methodHandlerImpl.getMockHandler();
        return mockHandler;
    }

    public void resetMock(Object mock, MockHandler mockHandler, @SuppressWarnings("rawtypes")
    MockCreationSettings mockCreationSettings)
    {
        MethodHandlerImpl methodHandler;
        if (mock instanceof ProxyObject)
        {
            ProxyObject proxyObject = (ProxyObject) mock;
            methodHandler = (MethodHandlerImpl) proxyObject.getHandler();
        }
        else
        {
            methodHandler = methodHandlers.get(mock);
        }
        methodHandler.setMockHandler(mockHandler);
        methodHandler.setMockCreationSettings(mockCreationSettings);
    }

}
