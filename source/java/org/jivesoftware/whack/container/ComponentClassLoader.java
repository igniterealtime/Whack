/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.whack.container;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ClassLoader for components. It searches the component directory for classes
 * and JAR files, then constructs a class loader for the resources found.
 * Resources are loaded as follows:<ul>
 *
 *      <li>Any JAR files in the <tt>lib</tt> will be added to the classpath.
 *      <li>Any files in the classes directory will be added to the classpath.
 * </ul>
 *
 * @author Derek DeMoro
 * @author Gaston Dombiak
 */
class ComponentClassLoader {

    private URLClassLoader classLoader;

    /**
     * Constructs a component loader for the given component directory.
     *
     * @param componentDir the component directory.
     * @throws java.lang.SecurityException if the created class loader violates
     *      existing security constraints.
     * @throws java.net.MalformedURLException if a located resource name cannot be
     *      properly converted to a URL.
     */
    public ComponentClassLoader(File componentDir) throws MalformedURLException, SecurityException {
        final List list = new ArrayList();
        File classesDir = new File(componentDir, "classes");
        if (classesDir.exists()) {
            list.add(classesDir.toURI().toURL());
        }
        File libDir = new File(componentDir, "lib");
        File[] jars = libDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") || name.endsWith(".zip");
            }
        });
        if (jars != null) {
            for (int i = 0; i < jars.length; i++) {
                if (jars[i] != null && jars[i].isFile()) {
                    list.add(jars[i].toURI().toURL());
                }
            }
        }
        Iterator urls = list.iterator();
        URL[] urlArray = new URL[list.size()];
        for (int i = 0; urls.hasNext(); i++) {
            urlArray[i] = (URL)urls.next();
        }
        classLoader = new URLClassLoader(urlArray, findParentClassLoader());
    }

    /**
     * Load a class using this component class loader.
     *
     * @param name the fully qualified name of the class to load.
     * @return The module object loaded
     * @throws ClassNotFoundException if the class could not be loaded by this class loader.
     * @throws IllegalAccessException if the class constructor was private or protected.
     * @throws InstantiationException if the class could not be instantiated (initialization error).
     * @throws SecurityException if the custom class loader not allowed.
     */
    public Class loadClass(String name) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, SecurityException
    {
        return classLoader.loadClass(name);
    }

    /**
     * Destroys this class loader.
     */
    public void destroy() {
        classLoader = null;
    }

    /**
     * Locates the best parent class loader based on context.
     *
     * @return the best parent classloader to use.
     */
    private ClassLoader findParentClassLoader() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = this.getClass().getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return parent;
    }
}