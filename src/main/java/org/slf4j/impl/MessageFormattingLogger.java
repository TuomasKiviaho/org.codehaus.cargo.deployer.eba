/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.slf4j.impl;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

public abstract class MessageFormattingLogger extends MarkerIgnoringBase {

  private static final long serialVersionUID = -4769121644461906796L;

  /**
   * This is our internal implementation for logging regular (non-parameterized)
   * log messages.
   * 
   * @param level
   *          One of the LOG_LEVEL_XXX constants defining the log level
   * @param message
   *          The message itself
   * @param t
   *          The exception whose stack trace should be logged
   */
  protected abstract void log(int level, String message, Throwable t);

  /**
   * For formatted messages, first substitute arguments and then log.
   * 
   * @param level
   * @param format
   * @param param1
   * @param param2
   */
  private void formatAndLog(int level, String format, Object arg1, Object arg2) {
    if (!isLevelEnabled(level)) {
      return;
    }
    FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  /**
   * For formatted messages, first substitute arguments and then log.
   * 
   * @param level
   * @param format
   * @param argArray
   */
  private void formatAndLog(int level, String format, Object[] argArray) {
    if (!isLevelEnabled(level)) {
      return;
    }
    FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  /**
   * Is the given log level currently enabled?
   * 
   * @param logLevel
   *          is this level enabled?
   */
  protected boolean isLevelEnabled(int logLevel) {
    return true;
  }

  /**
   * Are {@code trace} messages currently enabled?
   */
  public boolean isTraceEnabled() {
    return isLevelEnabled(LocationAwareLogger.TRACE_INT);
  }

  /**
   * A simple implementation which logs messages of level TRACE according to the
   * format outlined above.
   */
  public void trace(String msg) {
    log(LocationAwareLogger.TRACE_INT, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object param1) {
    formatAndLog(LocationAwareLogger.TRACE_INT, format, param1, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object param1, Object param2) {
    formatAndLog(LocationAwareLogger.TRACE_INT, format, param1, param2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object[] argArray) {
    formatAndLog(LocationAwareLogger.TRACE_INT, format, argArray);
  }

  /**
   * Log a message of level TRACE, including an exception.
   */
  public void trace(String msg, Throwable t) {
    log(LocationAwareLogger.TRACE_INT, msg, t);
  }

  /**
   * Are {@code debug} messages currently enabled?
   */
  public boolean isDebugEnabled() {
    return isLevelEnabled(LocationAwareLogger.DEBUG_INT);
  }

  /**
   * A simple implementation which logs messages of level DEBUG according to the
   * format outlined above.
   */
  public void debug(String msg) {
    log(LocationAwareLogger.DEBUG_INT, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object param1) {
    formatAndLog(LocationAwareLogger.DEBUG_INT, format, param1, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object param1, Object param2) {
    formatAndLog(LocationAwareLogger.DEBUG_INT, format, param1, param2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object[] argArray) {
    formatAndLog(LocationAwareLogger.DEBUG_INT, format, argArray);
  }

  /**
   * Log a message of level DEBUG, including an exception.
   */
  public void debug(String msg, Throwable t) {
    log(LocationAwareLogger.DEBUG_INT, msg, t);
  }

  /**
   * Are {@code info} messages currently enabled?
   */
  public boolean isInfoEnabled() {
    return isLevelEnabled(LocationAwareLogger.INFO_INT);
  }

  /**
   * A simple implementation which logs messages of level INFO according to the
   * format outlined above.
   */
  public void info(String msg) {
    log(LocationAwareLogger.INFO_INT, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object arg) {
    formatAndLog(LocationAwareLogger.INFO_INT, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object arg1, Object arg2) {
    formatAndLog(LocationAwareLogger.INFO_INT, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object[] argArray) {
    formatAndLog(LocationAwareLogger.INFO_INT, format, argArray);
  }

  /**
   * Log a message of level INFO, including an exception.
   */
  public void info(String msg, Throwable t) {
    log(LocationAwareLogger.INFO_INT, msg, t);
  }

  /**
   * Are {@code warn} messages currently enabled?
   */
  public boolean isWarnEnabled() {
    return isLevelEnabled(LocationAwareLogger.WARN_INT);
  }

  /**
   * A simple implementation which always logs messages of level WARN according
   * to the format outlined above.
   */
  public void warn(String msg) {
    log(LocationAwareLogger.WARN_INT, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object arg) {
    formatAndLog(LocationAwareLogger.WARN_INT, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object arg1, Object arg2) {
    formatAndLog(LocationAwareLogger.WARN_INT, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object[] argArray) {
    formatAndLog(LocationAwareLogger.WARN_INT, format, argArray);
  }

  /**
   * Log a message of level WARN, including an exception.
   */
  public void warn(String msg, Throwable t) {
    log(LocationAwareLogger.WARN_INT, msg, t);
  }

  /**
   * Are {@code error} messages currently enabled?
   */
  public boolean isErrorEnabled() {
    return isLevelEnabled(LocationAwareLogger.ERROR_INT);
  }

  /**
   * A simple implementation which always logs messages of level ERROR according
   * to the format outlined above.
   */
  public void error(String msg) {
    log(LocationAwareLogger.ERROR_INT, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object arg) {
    formatAndLog(LocationAwareLogger.ERROR_INT, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object arg1, Object arg2) {
    formatAndLog(LocationAwareLogger.ERROR_INT, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object[] argArray) {
    formatAndLog(LocationAwareLogger.ERROR_INT, format, argArray);
  }

  /**
   * Log a message of level ERROR, including an exception.
   */
  public void error(String msg, Throwable t) {
    log(LocationAwareLogger.ERROR_INT, msg, t);
  }
}