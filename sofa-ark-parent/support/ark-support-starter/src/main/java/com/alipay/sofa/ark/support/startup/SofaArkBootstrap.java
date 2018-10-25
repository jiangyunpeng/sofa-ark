/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.ark.support.startup;

import com.alipay.sofa.ark.bootstrap.ClasspathLauncher;
import com.alipay.sofa.ark.bootstrap.ClasspathLauncher.ClassPathArchive;
import com.alipay.sofa.ark.common.util.AssertUtils;
import com.alipay.sofa.ark.spi.argument.CommandArgument;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * relaunch a started main method with bootstrapping ark container
 *
 * @author qilong.zql
 * @since 0.1.0
 */
public class SofaArkBootstrap {

    private static final String BIZ_CLASSLOADER = "com.alipay.sofa.ark.container.service.classloader.BizClassLoader";
    private static final String MAIN_ENTRY_NAME = "remain";
    private static EntryMethod  entryMethod;

    public static void launch(String[] args) {
        try {
            if (!isSofaArkStarted()) {

                Thread main = Thread.currentThread();
                entryMethod = new EntryMethod(main); //  The "main" method located from a running thread.

                ClassLoader classLoader = main.getContextClassLoader();
                Class<?> startClass = classLoader.loadClass(SofaArkBootstrap.class.getName());

                Method entryMethod;
                try {
                    entryMethod = startClass.getMethod(MAIN_ENTRY_NAME, String[].class);
                } catch (NoSuchMethodException ex) {
                    entryMethod = startClass.getDeclaredMethod(MAIN_ENTRY_NAME, String[].class);
                }

                if (!entryMethod.isAccessible()) {
                    entryMethod.setAccessible(true);
                }

                entryMethod.invoke(null, new Object[] { args });

                // check if there is no daemonThread
                join(main.getThreadGroup(), main);
                System.exit(0);

            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void join(ThreadGroup threadGroup, Thread main) {
        boolean hasNonDaemonThreads;
        do {
            hasNonDaemonThreads = false;
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (Thread thread : threads) {
                if (thread != null && !thread.isDaemon() && !thread.equals(main)) {
                    try {
                        hasNonDaemonThreads = true;
                        thread.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (hasNonDaemonThreads);
    }

    public static Object prepareContainerForTest() {
        try {
            /* default set sofa-ark log configuration to 'dev' mode when startup in IDE */
            System.setProperty("log.env.suffix", "com.alipay.sofa.ark:dev");

            URL[] urls = getURLClassPath();
            return new ClasspathLauncher(new ClassPathArchive(urls)).launch(getClasspath(urls));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void remain(String[] args) throws Exception {// NOPMD

        AssertUtils.assertNotNull(entryMethod, "No Entry Method Found.");

        /* default set sofa-ark log configuration to 'dev' mode when startup in IDE */
        System.setProperty("log.env.suffix", "com.alipay.sofa.ark:dev");

        URL[] urls = getURLClassPath();
        new ClasspathLauncher(new ClassPathArchive(urls)).launch(args, getClasspath(urls),
            entryMethod.getMethod());

    }

    private static String getClasspath(URL[] urls) {

        StringBuilder sb = new StringBuilder();
        for (URL url : urls) {
            sb.append(url.toExternalForm()).append(CommandArgument.CLASSPATH_SPLIT);
        }

        return sb.toString();
    }

    private static URL[] getURLClassPath() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return ((URLClassLoader) classLoader).getURLs();
    }

    private static boolean isSofaArkStarted() {
        Class<?> bizClassloader = SofaArkBootstrap.class.getClassLoader().getClass();
        return BIZ_CLASSLOADER.equals(bizClassloader.getCanonicalName());
    }

}