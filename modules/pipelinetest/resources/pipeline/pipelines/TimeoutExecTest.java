/*
 * Copyright (c) 2020 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
class TimeoutExecTest {
    public static void main(String[] args) throws Exception
    {
        println("hello java timeout world!");
        println("arguments.length: " + args.length);
        var timeout = 10;
        for (var i = 0; i < args.length; i++)
        {
            println("  arg[" + i + "]=" + args[i]);
            if ("-t".equals(args[i]))
            {
                timeout = Integer.parseInt(args[i+1]);
            }
        }

        println("sleeping for " + timeout + " seconds");
        Thread.sleep(1000 * timeout);
        println("goodbye java timeout world!");
    }

    public static void println(String s)
    {
        System.out.println(s);
    }
}
