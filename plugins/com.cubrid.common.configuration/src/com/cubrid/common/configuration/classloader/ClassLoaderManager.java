/*
 * Copyright (C) 2008 Search Solution Corporation.
 * Copyright (C) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.common.configuration.classloader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ClassLoaderManager Description
 *
 * @author Kevin Cao
 * @version 1.0 - 2014-2-13 created by Kevin Cao
 */
public final class ClassLoaderManager {

    private static final ClassLoaderManager MANGER = new ClassLoaderManager();

    private static final String ORACLE_I18N_JAR = "orai18n.jar";

    private Map<String, ClassLoader> path2Loader = new HashMap<String, ClassLoader>();

    public static final ClassLoaderManager getInstance() {
        return MANGER;
    }

    /**
     * getClassLoader
     *
     * @param file full name of a file
     * @return URLClassLoader
     */
    public ClassLoader getClassLoader(String file) {
        synchronized (this) {
            try {
                final URL[] us;
                File file2 = new File(file);
                ClassLoader result = path2Loader.get(file2.getCanonicalPath());
                if (result != null) {
                    return result;
                }
                us = buildClassPathUrls(file2);
                result =
                        AccessController.doPrivileged(
                                new PrivilegedAction<URLClassLoader>() {
                                    public URLClassLoader run() {
                                        return new URLClassLoader(us);
                                    }
                                });
                path2Loader.put(file2.getCanonicalPath(), result);
                return result;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private URL[] buildClassPathUrls(File driverFile) throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(driverFile.toURI().toURL());

        if (isOracleJdbcDriver(driverFile)) {
            File parent = driverFile.getParentFile();
            if (parent != null) {
                File companionJar = new File(parent, ORACLE_I18N_JAR);
                if (companionJar.isFile()
                        && !companionJar.getCanonicalPath().equals(driverFile.getCanonicalPath())) {
                    urls.add(companionJar.toURI().toURL());
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private boolean isOracleJdbcDriver(File driverFile) {
        String name = driverFile.getName().toLowerCase(Locale.ENGLISH);
        return name.startsWith("ojdbc") && name.endsWith(".jar");
    }
}
