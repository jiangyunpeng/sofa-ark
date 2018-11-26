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
package com.alipay.sofa.ark.common.util;

import com.alipay.sofa.ark.exception.ArkException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Classloader Util
 *
 * @author ruoshan
 * @since 0.1.0
 */
public class ClassloaderUtils {

    private static final String JAVA_AGENT_MARK        = "-javaagent:";

    private static final String JAVA_AGENT_OPTION_MARK = "=";

    /**
     * push ContextClassloader
     * @param newClassloader new classloader
     * @return old classloader
     */
    public static ClassLoader pushContextClassloader(ClassLoader newClassloader) {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(newClassloader);
        return oldClassloader;
    }

    /**
     * set ContextClassloader back
     * @param oldClassloader old classloader
     */
    public static void popContextClassloader(ClassLoader oldClassloader) {
        Thread.currentThread().setContextClassLoader(oldClassloader);
    }

    public static URL[] getAgentClassPath() {
        List<String> inputArguments = AccessController
            .doPrivileged(new PrivilegedAction<List<String>>() {
                @Override
                public List<String> run() {
                    return ManagementFactory.getRuntimeMXBean().getInputArguments();
                }
            });

        List<URL> agentPaths = new ArrayList<>();
        for (String argument : inputArguments) {
            if (!argument.startsWith(JAVA_AGENT_MARK)) {
                continue;
            }
            argument = argument.substring(JAVA_AGENT_MARK.length());
            try {
                String path = argument.split(JAVA_AGENT_OPTION_MARK)[0];
                URL url = new File(path).toURI().toURL();
                agentPaths.add(url);
            } catch (Throwable e) {
                throw new ArkException("Failed to create java agent classloader", e);
            }
        }
        return agentPaths.toArray(new URL[] {});
    }

}