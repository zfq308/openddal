/*
 * Copyright 2014-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openddal.message;

import java.text.MessageFormat;
import java.util.ArrayList;

import com.openddal.command.expression.ParameterInterface;
import com.openddal.util.StatementBuilder;
import com.openddal.util.StringUtils;
import com.openddal.value.Value;

/**
 * This class represents a trace module.
 */
public class Trace {

    /**
     * The trace module name for commands.
     */
    public static final String COMMAND = "command";
    
    /**
     * The trace module name for databases.
     */
    public static final String DATABASE = "database";

    /**
     * The trace module name for functions.
     */
    public static final String FUNCTION = "function";

    /**
     * The trace module name for the JDBC API.
     */
    public static final String JDBC = "jdbc";
    
    /**
     * The trace module name for schemas.
     */
    public static final String SCHEMA = "schema";

    /**
     * The trace module name for sequences.
     */
    public static final String SEQUENCE = "sequence";

    /**
     * The trace module name for executor.
     */
    public static final String EXECUTOR = "executor";

    /**
     * The trace module name for transaction.
     */
    public static final String TRANSACTION = "transaction";

    /**
     * The trace module name for datasource.
     */
    public static final String REPOSITORY = "repository";

    private final TraceWriter traceWriter;
    private int traceLevel = TraceSystem.PARENT;

    Trace(TraceWriter traceWriter) {
        this.traceWriter = traceWriter;
    }

    /**
     * Format the parameter list.
     *
     * @param parameters the parameter list
     * @return the formatted text
     */
    public static String formatParams(
            ArrayList<? extends ParameterInterface> parameters) {
        if (parameters.size() == 0) {
            return "";
        }
        StatementBuilder buff = new StatementBuilder();
        int i = 0;
        boolean params = false;
        for (ParameterInterface p : parameters) {
            if (p.isValueSet()) {
                if (!params) {
                    buff.append(" {");
                    params = true;
                }
                buff.appendExceptFirst(", ");
                Value v = p.getParamValue();
                buff.append(++i).append(": ").append(v.getTraceSQL());
            }
        }
        if (params) {
            buff.append('}');
        }
        return buff.toString();
    }

    /**
     * Set the trace level of this component. This setting overrides the parent
     * trace level.
     *
     * @param level the new level
     */
    public void setLevel(int level) {
        this.traceLevel = level;
    }

    private boolean isEnabled(int level) {
        if (this.traceLevel == TraceSystem.PARENT) {
            return traceWriter.isEnabled(level);
        }
        return level <= this.traceLevel;
    }

    /**
     * Check if the trace level is equal or higher than INFO.
     *
     * @return true if it is
     */
    public boolean isInfoEnabled() {
        return isEnabled(TraceSystem.INFO);
    }

    /**
     * Check if the trace level is equal or higher than DEBUG.
     *
     * @return true if it is
     */
    public boolean isDebugEnabled() {
        return isEnabled(TraceSystem.DEBUG);
    }

    /**
     * Write a message with trace level ERROR to the trace system.
     *
     * @param t the exception
     * @param s the message
     */
    public void error(Throwable t, String s) {
        if (isEnabled(TraceSystem.ERROR)) {
            traceWriter.write(TraceSystem.ERROR, s, t);
        }
    }

    /**
     * Write a message with trace level ERROR to the trace system.
     *
     * @param t      the exception
     * @param s      the message
     * @param params the parameters
     */
    public void error(Throwable t, String s, Object... params) {
        if (isEnabled(TraceSystem.ERROR)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.ERROR, s, t);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param s the message
     */
    public void info(String s) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, s, null);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param s      the message
     * @param params the parameters
     */
    public void info(String s, Object... params) {
        if (isEnabled(TraceSystem.INFO)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.INFO, s, null);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param t the exception
     * @param s the message
     */
    void info(Throwable t, String s) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, s, t);
        }
    }

    /**
     * Write a SQL statement with trace level INFO to the trace system.
     *
     * @param sql    the SQL statement
     * @param params the parameters used, in the for {1:...}
     * @param count  the update count
     * @param time   the time it took to run the statement in ms
     */
    public void infoSQL(String sql, String params, int count, long time) {
        if (!isEnabled(TraceSystem.INFO)) {
            return;
        }
        StringBuilder buff = new StringBuilder(sql.length() + params.length() + 20);
        buff.append("/*SQL");
        boolean space = false;
        if (params.length() > 0) {
            // This looks like a bug, but it is intentional:
            // If there are no parameters, the SQL statement is
            // the rest of the line. If there are parameters, they
            // are appended at the end of the line. Knowing the size
            // of the statement simplifies separating the SQL statement
            // from the parameters (no need to parse).
            space = true;
            buff.append(" l:").append(sql.length());
        }
        if (count > 0) {
            space = true;
            buff.append(" r:").append(count);
        }
        if (time > 0) {
            space = true;
            buff.append(" t:").append(time);
        }
        if (!space) {
            buff.append(' ');
        }
        buff.append("*/").
                append(StringUtils.javaEncode(sql)).
                append(StringUtils.javaEncode(params)).
                append(';');
        sql = buff.toString();
        traceWriter.write(TraceSystem.INFO, sql, null);
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param s      the message
     * @param params the parameters
     */
    public void debug(String s, Object... params) {
        if (isEnabled(TraceSystem.DEBUG)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.DEBUG, s, null);
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param s the message
     */
    public void debug(String s) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, s, null);
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param t the exception
     * @param s the message
     */
    public void debug(Throwable t, String s) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, s, t);
        }
    }


    /**
     * Write Java source code with trace level INFO to the trace system.
     *
     * @param java the source code
     */
    public void infoCode(String java) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, java, null);
        }
    }

    /**
     * Write Java source code with trace level DEBUG to the trace system.
     *
     * @param java the source code
     */
    void debugCode(String java) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, java, null);
        }
    }

}
