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

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Loads and manages components. The <tt>components</tt> directory is monitored for any
 * new components, and they are dynamically loaded.<p>
 *
 * @see Component
 * @see ServerContainer#start()
 * @author Matt Tucker
 * @author Gaston Dombiak
 */
public class ComponentFinder {

    private File componentDirectory;
    private Map<String,Component> components;
    private Map<Component,ComponentClassLoader> classloaders;
    private Map<Component,File> componentDirs;
    private Map<Component, String> componentDomains;
    private boolean setupMode = true;
    private ExternalComponentManager manager;
    private ScheduledExecutorService executor = null;

    /**
     * Constructs a new component manager.
     *
     * @param componentDir the component directory.
     */
    public ComponentFinder(ServerContainer server, File componentDir) {
        this.componentDirectory = componentDir;
        components = new HashMap<String,Component>();
        componentDirs = new HashMap<Component,File>();
        classloaders = new HashMap<Component,ComponentClassLoader>();
        componentDomains = new HashMap<Component,String>();
        manager = (ExternalComponentManager) server.getManager();
        setupMode = server.isSetupMode();
    }

    /**
     * Starts the service that looks for components.
     */
    public void start() {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleWithFixedDelay(new ComponentMonitor(), 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Shuts down running components that were found by the service.
     */
    public void shutdown() {
        // Stop the component monitoring service.
        if (executor != null) {
            executor.shutdown();
        }
        // Shutdown found components.
        for (String subdomain : componentDomains.values()) {
            try {
                manager.removeComponent(subdomain);
            } catch (ComponentException e) {
                manager.getLog().error("Error shutting down component", e);
            }
        }
        components.clear();
        componentDirs.clear();
        classloaders.clear();
        componentDomains.clear();
    }

    /**
     * Returns a Collection of all found components.
     *
     * @return a Collection of all found components.
     */
    public Collection<Component> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * Returns a component by name or <tt>null</tt> if a component with that name does not
     * exist. The name is the name of the directory that the component is in such as
     * "broadcast".
     *
     * @param name the name of the component.
     * @return the component.
     */
    public Component getComponent(String name) {
        return components.get(name);
    }

    /**
     * Returns the component's directory.
     *
     * @param component the component.
     * @return the component's directory.
     */
    public File getComponentDirectory(Component component) {
        return componentDirs.get(component);
    }

    /**
     * Loads a plug-in module into the container. Loading consists of the
     * following steps:<ul>
     *
     *      <li>Add all jars in the <tt>lib</tt> dir (if it exists) to the class loader</li>
     *      <li>Add all files in <tt>classes</tt> dir (if it exists) to the class loader</li>
     *      <li>Locate and load <tt>module.xml</tt> into the context</li>
     *      <li>For each jive.module entry, load the given class as a module and start it</li>
     *
     * </ul>
     *
     * @param componentDir the component directory.
     */
    private void loadComponent(File componentDir) {
        // Do not load & start any component if in setup mode
        if (setupMode) {
            return;
        }
        manager.getLog().debug("Loading component: " + componentDir.getName());
        Component component = null;
        try {
            File componentConfig = new File(componentDir, "component.xml");
            if (componentConfig.exists()) {
                SAXReader saxReader = new SAXReader();
                Document componentXML = saxReader.read(componentConfig);
                ComponentClassLoader classLoader = new ComponentClassLoader(componentDir);
                String className = componentXML.selectSingleNode("/component/class").getText();
                String subdomain = componentXML.selectSingleNode("/component/subdomain").getText();
                //component = (Component)classLoader.loadClass(className).newInstance();
                Class aClass = classLoader.loadClass(className);
                component = (Component)aClass.newInstance();

                manager.addComponent(subdomain, component);

                components.put(componentDir.getName(), component);
                componentDirs.put(component, componentDir);
                classloaders.put(component, classLoader);
                componentDomains.put(component, subdomain);
                // Load any JSP's defined by the component.
                File webXML = new File(componentDir, "web" + File.separator + "web.xml");
                if (webXML.exists()) {
                    ComponentServlet.registerServlets(this, component, webXML);
                }
                // If there a <adminconsole> section defined, register it.
                Element adminElement = (Element)componentXML.selectSingleNode("/component/adminconsole");
                if (adminElement != null) {
                    // If global images are specified, override their URL.
                    Element imageEl = (Element)adminElement.selectSingleNode(
                            "/component/adminconsole/global/logo-image");
                    if (imageEl != null) {
                        imageEl.setText("components/" + componentDir.getName() + "/" + imageEl.getText());
                    }
                    imageEl = (Element)adminElement.selectSingleNode(
                            "/component/adminconsole/global/login-image");
                    if (imageEl != null) {
                        imageEl.setText("components/" + componentDir.getName() + "/" + imageEl.getText());
                    }
                    // Modify all the URL's in the XML so that they are passed through
                    // the component servlet correctly.
                    /*List urls = adminElement.selectNodes("//@url");
                    for (int i=0; i<urls.size(); i++) {
                        Attribute attr = (Attribute)urls.get(i);
                        attr.setValue("components/" + componentDir.getName() + "/" + attr.getValue());
                    }
                    AdminConsole.addModel(componentDir.getName(), adminElement);*/
                }
            }
            else {
                manager.getLog().warn("Component " + componentDir + " could not be loaded: no component.xml file found");
            }
        }
        catch (Exception e) {
            manager.getLog().error("Error loading component: " + componentDir.getName(), e);
        }
    }

    /**
     * Unloads a component. The {@link ComponentManager#removeComponent(String)} method will be
     * called and then any resources will be released. The name should be the name of the component
     * directory and not the name as given by the component meta-data. This method only removes
     * the component but does not delete the component JAR file. Therefore, if the component JAR
     * still exists after this method is called, the component will be started again the next
     * time the component monitor process runs. This is useful for "restarting" components.<p>
     *
     * This method is called automatically when a component's JAR file is deleted.
     *
     * @param componentName the name of the component to unload.
     */
    public void unloadComponent(String componentName) {
        manager.getLog().debug("Unloading component " + componentName);
        Component component = components.get(componentName);
        if (component == null) {
            return;
        }
        File webXML = new File(componentDirectory + File.separator + componentName +
                File.separator + "web" + File.separator + "web.xml");
        if (webXML.exists()) {
            //AdminConsole.removeModel(componentName);
            ComponentServlet.unregisterServlets(webXML);
        }

        ComponentClassLoader classLoader = classloaders.get(component);
        try {
            manager.removeComponent(componentDomains.get(component));
        } catch (ComponentException e) {
            manager.getLog().error("Error shutting down component", e);
        }
        classLoader.destroy();
        components.remove(componentName);
        componentDirs.remove(component);
        classloaders.remove(component);
        componentDomains.remove(component);
    }

    public Class loadClass(String className, Component component) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException
    {
        ComponentClassLoader loader = classloaders.get(component);
        return loader.loadClass(className);
    }

    /**
     * Returns the name of a component. The value is retrieved from the component.xml file
     * of the component. If the value could not be found, <tt>null</tt> will be returned.
     * Note that this value is distinct from the name of the component directory.
     *
     * @param component the component.
     * @return the component's name.
     */
    public String getName(Component component) {
        String name = getElementValue(component, "/component/name");
        if (name != null) {
            return name;
        }
        else {
            return componentDirs.get(component).getName();
        }
    }

    /**
     * Returns the description of a component. The value is retrieved from the component.xml file
     * of the component. If the value could not be found, <tt>null</tt> will be returned.
     *
     * @param component the component.
     * @return the component's description.
     */
    public String getDescription(Component component) {
        return getElementValue(component, "/component/description");
    }

    /**
     * Returns the author of a component. The value is retrieved from the component.xml file
     * of the component. If the value could not be found, <tt>null</tt> will be returned.
     *
     * @param component the component.
     * @return the component's author.
     */
    public String getAuthor(Component component) {
        return getElementValue(component, "/component/author");
    }

    /**
     * Returns the version of a component. The value is retrieved from the component.xml file
     * of the component. If the value could not be found, <tt>null</tt> will be returned.
     *
     * @param component the component.
     * @return the component's version.
     */
    public String getVersion(Component component) {
        return getElementValue(component, "/component/version");
    }

    /**
     * Returns the value of an element selected via an xpath expression from
     * a component's component.xml file.
     *
     * @param component the component.
     * @param xpath the xpath expression.
     * @return the value of the element selected by the xpath expression.
     */
    private String getElementValue(Component component, String xpath) {
        File componentDir = componentDirs.get(component);
        if (componentDir == null) {
            return null;
        }
        try {
            File componentConfig = new File(componentDir, "component.xml");
            if (componentConfig.exists()) {
                SAXReader saxReader = new SAXReader();
                Document componentXML = saxReader.read(componentConfig);
                Element element = (Element)componentXML.selectSingleNode(xpath);
                if (element != null) {
                    return element.getTextTrim();
                }
            }
        }
        catch (Exception e) {
            manager.getLog().error(e);
        }
        return null;
    }

    /**
     * A service that monitors the component directory for components. It periodically
     * checks for new component JAR files and extracts them if they haven't already
     * been extracted. Then, any new component directories are loaded.
     */
    private class ComponentMonitor implements Runnable {

        public void run() {
            try {
                File [] jars = componentDirectory.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        String fileName = pathname.getName().toLowerCase();
                        return (fileName.endsWith(".jar") || fileName.endsWith(".war"));
                    }
                });

                for (int i=0; i<jars.length; i++) {
                    File jarFile = jars[i];
                    String componentName = jarFile.getName().substring(
                            0, jarFile.getName().length()-4).toLowerCase();
                    // See if the JAR has already been exploded.
                    File dir = new File(componentDirectory, componentName);
                    // If the JAR hasn't been exploded, do so.
                    if (!dir.exists()) {
                        unzipComponent(componentName, jarFile, dir);
                    }
                    // See if the JAR is newer than the directory. If so, the component
                    // needs to be unloaded and then reloaded.
                    else if (jarFile.lastModified() > dir.lastModified()) {
                        unloadComponent(componentName);
                        // Ask the system to clean up references.
                        System.gc();
                        while (!deleteDir(dir)) {
                            manager.getLog().error("Error unloading component " + componentName + ". " +
                                    "Will attempt again momentarily.");
                            Thread.sleep(5000);
                        }
                        // Now unzip the component.
                        unzipComponent(componentName, jarFile, dir);
                    }
                }

                File [] dirs = componentDirectory.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });

                for (int i=0; i<dirs.length; i++) {
                    File dirFile = dirs[i];
                    // If the component hasn't already been started, start it.
                    if (!components.containsKey(dirFile.getName())) {
                        loadComponent(dirFile);
                    }
                }

                // See if any currently running components need to be unloaded
                // due to its JAR file being deleted.
                if (components.size() > jars.length + 1) {
                    // Build a list of components to delete first so that the components
                    // keyset is modified as we're iterating through it.
                    List<String> toDelete = new ArrayList<String>();
                    for (String componentName : components.keySet()) {
                        File file = new File(componentDirectory, componentName + ".jar");
                        if (!file.exists()) {
                            toDelete.add(componentName);
                        }
                    }
                    for (String componentName : toDelete) {
                        unloadComponent(componentName);
                        System.gc();
                        while (!deleteDir(new File(componentDirectory, componentName))) {
                            manager.getLog().error("Error unloading component " + componentName + ". " +
                                    "Will attempt again momentarily.");
                            Thread.sleep(5000);
                        }
                    }
                }
            }
            catch (Exception e) {
                manager.getLog().error(e);
            }
        }

        /**
         * Unzips a component from a JAR file into a directory. If the JAR file
         * isn't a component, this method will do nothing.
         *
         * @param componentName the name of the component.
         * @param file the JAR file
         * @param dir the directory to extract the component to.
         */
        private void unzipComponent(String componentName, File file, File dir) {
            try {
                ZipFile zipFile = new JarFile(file);
                // Ensure that this JAR is a component.
                if (zipFile.getEntry("component.xml") == null) {
                    return;
                }
                dir.mkdir();
                manager.getLog().debug("Extracting component: " + componentName);
                for (Enumeration e=zipFile.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = (JarEntry)e.nextElement();
                    File entryFile = new File(dir, entry.getName());
                    // Ignore any manifest.mf entries.
                    if (entry.getName().toLowerCase().endsWith("manifest.mf")) {
                        continue;
                    }
                    if (!entry.isDirectory()) {
                        entryFile.getParentFile().mkdirs();
                        FileOutputStream out = new FileOutputStream(entryFile);
                        InputStream zin = zipFile.getInputStream(entry);
                        byte [] b = new byte[512];
                        int len = 0;
                        while ( (len=zin.read(b))!= -1 ) {
                            out.write(b,0,len);
                        }
                        out.flush();
                        out.close();
                        zin.close();
                    }
                }
                zipFile.close();
                zipFile = null;
            }
            catch (Exception e) {
                manager.getLog().error(e);
            }
        }

        /**
         * Deletes a directory.
         */
         public boolean deleteDir(File dir) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i=0; i<children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        }
    }
}
