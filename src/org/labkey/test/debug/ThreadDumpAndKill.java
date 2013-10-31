/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.debug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.tools.jdi.SocketAttachingConnector;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;

/**
 * Utility program to connect to JVM that is listening on a debug port via TCP/IP sockets, dump the threads, and kill
 * the VM.
 * User: jeckels
 * Date: 11/29/12
 */
public class ThreadDumpAndKill
{
    public static void main(String... args) throws IOException, IllegalConnectorArgumentsException, AbsentInformationException, IncompatibleThreadStateException
    {
        if (args.length != 1)
        {
            printUsage();
        }
        try
        {
            int port = Integer.parseInt(args[0]);
            for (AttachingConnector connector : Bootstrap.virtualMachineManager().attachingConnectors())
            {
                if (connector instanceof SocketAttachingConnector)
                {
                    connect((SocketAttachingConnector)connector, port);
                    System.exit(0);
                }
            }
            System.err.println("No SocketAttachingConnector found!");
            System.exit(1);
        }
        catch (NumberFormatException e)
        {
            printUsage();
        }
    }

    private static void printUsage()
    {
        System.err.println("Expected argument: <port number>");
        System.err.println("Where <port number> is the integer port number on which the target VM is listening");
        System.exit(1);
    }

    private static void connect(SocketAttachingConnector connector, int port) throws IllegalConnectorArgumentsException, IOException, AbsentInformationException, IncompatibleThreadStateException
    {
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue("localhost");
        arguments.get("port").setValue(Integer.toString(port));
        System.out.println("Attempting to shutdown Tomcat on debug port: " + port);
        try
        {
            VirtualMachine vm = connector.attach(arguments);
            vm.suspend();
            for (ThreadReference threadReference : vm.allThreads())
            {
                dumpThread(threadReference);
                System.out.println();
            }
            vm.resume();
            vm.exit(1);
            System.out.println("Killed remote VM");
        }
        catch (ConnectException e)
        {
            e.printStackTrace();
            System.out.println("Unable to connect to VM at localhost:" + port + ", VM may already be shut down");
        }
    }

    private static void dumpThread(ThreadReference threadReference) throws IncompatibleThreadStateException, AbsentInformationException
    {
        System.out.println("Thread '" + threadReference.name() + "', status = " + getStatus(threadReference));
        ObjectReference objectRef = threadReference.currentContendedMonitor();
        if (objectRef != null)
        {
            StringBuilder line = new StringBuilder();
            line.append("\t\tAttempting to acquire monitor for ");
            line.append(objectRef.referenceType().name());
            line.append("@");
            line.append(objectRef.uniqueID());
            if (objectRef.owningThread() != null)
            {
                line.append(" held by thread '");
                line.append(objectRef.owningThread().name());
                line.append("'");
            }
            System.out.println(line);
        }
        for (ObjectReference ownedMonitor : threadReference.ownedMonitors())
        {
            System.out.println("\t\tHolding monitor for " + ownedMonitor.referenceType().name() + "@" + ownedMonitor.uniqueID());
        }
        for (StackFrame stackFrame : threadReference.frames())
        {
            StringBuilder line = new StringBuilder();
            line.append("\t");
            line.append(stackFrame.location().declaringType().name());
            line.append(".").append(stackFrame.location().method().name());
            line.append("(");
            try
            {
                line.append(stackFrame.location().sourceName());
            }
            catch (AbsentInformationException e)
            {
                line.append("UnknownSource");
            }
            line.append(":").append(stackFrame.location().lineNumber());
            line.append(")");
            System.out.println(line.toString());
        }
    }

    private static String getStatus(ThreadReference threadReference)
    {
        switch (threadReference.status())
        {
            case ThreadReference.THREAD_STATUS_MONITOR:
                return "WAITING FOR MONITOR";
            case ThreadReference.THREAD_STATUS_NOT_STARTED:
                return "NOT STARTED";
            case ThreadReference.THREAD_STATUS_RUNNING:
                return "RUNNING";
            case ThreadReference.THREAD_STATUS_SLEEPING:
                return "SLEEPING";
            case ThreadReference.THREAD_STATUS_WAIT:
                return "WAITING";
            case ThreadReference.THREAD_STATUS_ZOMBIE:
                return "ZOMBIE";
            case ThreadReference.THREAD_STATUS_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }
}
