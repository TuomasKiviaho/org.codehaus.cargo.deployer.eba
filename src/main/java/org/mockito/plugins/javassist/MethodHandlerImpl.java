package org.mockito.plugins.javassist;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.RuntimeSupport;

import org.mockito.exceptions.Reporter;
import org.mockito.internal.creation.DelegatingMethod;
import org.mockito.internal.invocation.MockitoMethod;
import org.mockito.internal.invocation.SerializableMethod;
import org.mockito.internal.invocation.realmethod.RealMethod;
import org.mockito.internal.progress.SequenceNumber;
import org.mockito.internal.util.ObjectMethodsGuru;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

public class MethodHandlerImpl implements MethodHandler, Serializable
{

    private static class MockitoMethodRealMethod implements RealMethod, Serializable
    {

        private static final long serialVersionUID = 1083634094428018910L;

        private Object spiedInstance;

        private MockitoMethod mockitoMethod;

        public MockitoMethodRealMethod(Object spiedInstance, MockitoMethod mockitoMethod)
        {
            this.spiedInstance = spiedInstance;
            this.mockitoMethod = mockitoMethod;
        }

        public Object invoke(Object target, Object[] arguments) throws Throwable
        {
            if (this.mockitoMethod == null)
            {
                Reporter reporter = new Reporter();
                reporter.cannotCallRealMethodOnInterface();
            }
            Method method = this.mockitoMethod.getJavaMethod();
            int modifiers = method.getModifiers();
            return method.invoke(Modifier.isStatic(modifiers) ? null : spiedInstance == null
                ? target : spiedInstance, arguments);
        }

    }

    private static final long serialVersionUID = -4680561764460138092L;

    private static ObjectMethodsGuru objectMethodsGuru = new ObjectMethodsGuru();

    private MockHandler mockHandler;

    private MockCreationSettings< ? > mockCreationSettings;

    public MethodHandlerImpl()
    {
        super();
    }

    public MockCreationSettings< ? > getMockCreationSettings()
    {
        return this.mockCreationSettings;
    }

    public void setMockCreationSettings(MockCreationSettings< ? > mockCreationSettings)
    {
        this.mockCreationSettings = mockCreationSettings;
    }

    public MockHandler getMockHandler()
    {
        return this.mockHandler;
    }

    public void setMockHandler(MockHandler mockHandler)
    {
        this.mockHandler = mockHandler;
    }

    public Object invoke(final Object proxy, final Method proxyMethod, final Method typeMethod,
        final Object[] args) throws Throwable
    {
        Object result;
        if (objectMethodsGuru.isEqualsMethod(proxyMethod))
        {
            result = proxy == args[0];
        }
        else if (objectMethodsGuru.isHashCodeMethod(proxyMethod))
        {
            result = System.identityHashCode(proxy);
        }
        else
        {
            MockitoMethod mockitoMethod =
                this.mockCreationSettings.isSerializable() ? new SerializableMethod(proxyMethod)
                    : new DelegatingMethod(proxyMethod);
            int sequenceNumber = SequenceNumber.next();
            Method method = typeMethod;
            Object spiedInstance = mockCreationSettings.getSpiedInstance();
            if (spiedInstance != null)
            {
                String descriptor = RuntimeSupport.makeDescriptor(proxyMethod);
                String name = proxyMethod.getName();
                try
                {
                    method = RuntimeSupport.findMethod(spiedInstance, name, descriptor);
                }
                catch (RuntimeException e)
                {
                    try
                    {
                        method = RuntimeSupport.findSuperMethod(spiedInstance, name, descriptor);
                    }
                    catch (RuntimeException e2)
                    {
                        method = null;
                    }
                }
                Class< ? > declaringClass = method.getDeclaringClass();
                int modifiers = declaringClass.getModifiers();
                if (!Modifier.isPublic(modifiers))
                {
                    method.setAccessible(true);
                }
            }
            MockitoMethod typeMockitoMethod =
                method == null ? null : this.mockCreationSettings.isSerializable()
                    ? new SerializableMethod(method) : new DelegatingMethod(method);
            RealMethod realMethod =
                typeMockitoMethod == null ? null : new MockitoMethodRealMethod(spiedInstance,
                    typeMockitoMethod);
            Invocation invocation =
                new InvocationImpl(proxy, mockitoMethod, args, sequenceNumber, realMethod);
            result = this.mockHandler.handle(invocation);
        }
        return result;
    }

    public MockHandler getMockitoHandler()
    {
        return mockHandler;
    }

}
