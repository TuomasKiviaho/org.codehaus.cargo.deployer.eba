package org.mockito.plugins.javassist;

import org.mockito.exceptions.Reporter;
import org.mockito.internal.invocation.MockitoMethod;
import org.mockito.internal.invocation.realmethod.RealMethod;

public class InvocationImpl extends org.mockito.internal.invocation.InvocationImpl
{

    private static final long serialVersionUID = 436416403207198347L;

    private RealMethod realMethod;

    public InvocationImpl(Object mock, MockitoMethod mockitoMethod, Object[] args,
        int sequenceNumber, RealMethod realMethod)
    {
        super(mock, mockitoMethod, args, sequenceNumber, realMethod);
        this.realMethod = realMethod;
    }

    @Override
    public Object callRealMethod() throws Throwable
    {
        if (this.realMethod == null)
        {
            Reporter reporter = new Reporter();
            reporter.cannotCallRealMethodOnInterface();
        }
        Object mock = this.getMock();
        Object[] rawArguments = this.getRawArguments();
        return this.realMethod.invoke(mock, rawArguments);
    }

}
