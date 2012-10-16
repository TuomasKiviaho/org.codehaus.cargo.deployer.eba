package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.cargo.util.CargoException;
import org.codehaus.cargo.util.log.LogLevel;
import org.codehaus.cargo.util.log.Loggable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

public class LoggerFactoryImpl implements ILoggerFactory, Loggable
{

    private org.codehaus.cargo.util.log.Logger logger;

    private Map<String, Logger> loggers;

    public LoggerFactoryImpl()
    {
        this.loggers = new HashMap<String, Logger>();
    }

    public org.codehaus.cargo.util.log.Logger getLogger()
    {
        return this.logger;
    }

    public synchronized void setLogger(org.codehaus.cargo.util.log.Logger logger)
    {
        this.logger = logger;
    }

    public synchronized Logger getLogger(String name)
    {
        Logger logger = this.loggers.get(name);
        if (logger == null)
        {
            logger = new LoggerImpl(name)
            {

                private static final long serialVersionUID = 6920553805172092177L;

                @Override
                protected Loggable getLoggable()
                {
                    return LoggerFactoryImpl.this;
                }

            };
            this.loggers.put(name, logger);
        }
        return logger;
    }

}

abstract class LoggerImpl extends MessageFormattingLogger
{

    private static final long serialVersionUID = 5711791789247971287L;

    private static Map<LogLevel, Integer> LOG_LEVELS;

    {
        LOG_LEVELS = new HashMap<LogLevel, Integer>(3);
        LOG_LEVELS.put(LogLevel.DEBUG, LocationAwareLogger.DEBUG_INT);
        LOG_LEVELS.put(LogLevel.INFO, LocationAwareLogger.INFO_INT);
        LOG_LEVELS.put(LogLevel.WARN, LocationAwareLogger.WARN_INT);
    }

    public LoggerImpl(String name)
    {
        this.name = name;
    }

    protected abstract Loggable getLoggable();

    protected boolean isLevelEnabled(int logLevel)
    {
        Loggable loggable = this.getLoggable();
        org.codehaus.cargo.util.log.Logger logger = loggable.getLogger();
        Integer enabledLogLevel = null;
        if (logger != null)
        {
            LogLevel level = logger.getLevel();
            enabledLogLevel = LOG_LEVELS.get(level);
        }
        return enabledLogLevel != null && logLevel >= enabledLogLevel.intValue();
    }

    protected void log(int level, String message, Throwable throwable)
    {
        if (isLevelEnabled(level))
        {
            Loggable loggable = this.getLoggable();
            org.codehaus.cargo.util.log.Logger logger = loggable.getLogger();
            if (logger != null)
            {
                switch (level)
                {
                    case LocationAwareLogger.INFO_INT:
                        logger.info(message, this.name);
                        break;
                    case LocationAwareLogger.DEBUG_INT:
                        logger.debug(message, this.name);
                        break;
                    case LocationAwareLogger.WARN_INT:
                        logger.warn(message, this.name);
                        break;
                    default:
                        throw new CargoException(message, throwable);
                }
            }
        }
    }

}
