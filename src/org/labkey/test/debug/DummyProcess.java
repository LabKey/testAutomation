package org.labkey.test.debug;

import java.util.Random;

/**
 * User: jeckels
 * Date: 11/29/12
 */
public class DummyProcess
{
    public static void main(String... args)
    {
        new Thread(new Sleeper()).start();
        new Thread(new Sleeper()).start();
        new Thread(new Sleeper()).start();
        new Sleeper().run();
    }

    public static class Sleeper implements Runnable
    {
        @Override
        public void run()
        {
            while (true)
            {
                try { Thread.sleep(new Random().nextInt(1000)); } catch (InterruptedException ignored) {}
                try { Thread.sleep(new Random().nextInt(1000)); } catch (InterruptedException ignored) {}
                try { Thread.sleep(new Random().nextInt(1000)); } catch (InterruptedException ignored) {}
            }
        }
    }
}
