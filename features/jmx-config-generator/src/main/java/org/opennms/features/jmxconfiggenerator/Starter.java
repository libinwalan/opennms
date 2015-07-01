/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.jmxconfiggenerator;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.opennms.features.jmxconfiggenerator.graphs.GraphConfigGenerator;
import org.opennms.features.jmxconfiggenerator.graphs.JmxConfigReader;
import org.opennms.features.jmxconfiggenerator.graphs.Report;
import org.opennms.features.jmxconfiggenerator.jmxconfig.JmxDatacollectionConfiggenerator;
import org.opennms.xmlns.xsd.config.jmx_datacollection.JmxDatacollectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Simon Walter <simon.walter@hp-factory.de>
 * @author Markus Neumann <markus@opennms.com>
 */
public class Starter {

    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);
    
    @Option(name = "-jmx", usage = "Generate jmx-datacollection.xml by reading JMX over RMI")
    private boolean jmx = false;
    
    @Option(name = "-service", usage = "Your optional service-name. Like cassandra, jboss, tomcat")
    private String serviceName = "anyservice";

    @Option(name = "-host", usage = "Hostname or IP-Adress of JMX-RMI host", aliases = {"-hostname"})
    private String hostName;
    
    @Option(name = "-username", usage = "Username for JMX-RMI Authentication")
    private String username;
    
    @Option(name = "-password", usage = "Password for JMX-RMI Authentication")
    private String password;
    
    @Option(name = "-port", usage = "Port of JMX-RMI service")
    private String port;
    
    @Option(name = "-jmxmp", usage = "Use JMXMP and not JMX-RMI")
    private boolean jmxmp = false;
    
    // @Option(name = "-ssl", usage = "Use SSL for the connection")
    private boolean ssl = false;
    
    @Option(name = "-skipDefaultVM", usage = "set to process default JavaVM Beans.")
    private boolean skipDefaultVM = false;
    
    @Option(name = "-runWritableMBeans", usage = "include MBeans that are read- and writable.")
    private boolean runWritableMBeans = false;
    
    @Option(name = "-graph", usage = "Generate snmp-graph.properties linke file to out, by reading jmx-datacollection.xml like file from input")
    private boolean graph = false;
    
    @Option(name = "-input", usage = "Jmx-datacolletion.xml like file to parse")
    private String inputFile;
    
    @Option(name = "-out", usage = "File to write generated snmp-graph.properties linke content")
    private String outFile;
    
    @Option(name = "-template", usage = "Template file for SnmpGraphs")
    private String templateFile;
    
    @Option(name = "-dictionary", usage = "Dictionary properties file for replacing attribute names and parts of this names")
    private String dictionaryFile;
    
    @Option(name = "-url", usage = "JMX URL Usage: <hostname>:<port> OR service:jmx:<protocol>:<sap> OR service:jmx:remoting-jmx://<hostname>:<port>")
    private String url;

    // TODO MVR ....
    @Option(name ="-list", usage = "TODO")
    private boolean list;

    // TODO MVR ....
    @Option(name="-idFilter", usage ="TODO", aliases = {"-filter"})
    private String idFilter;

    public static void main(String[] args) throws IOException, CmdLineException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        new Starter().doMain(args);
    }

    public void doMain(String[] args) throws IOException, CmdLineException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        CmdLineParser parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(80));

        try {
            parser.parseArgument(args);
            // TODO MVR add validation again
//            if (jmx && graph) {
//                throw new CmdLineException(parser, "jmx and graph options are set. Just use one at a time.");
//            } else if (!jmx && !graph) {
//                throw new CmdLineException(parser, "No jmx or graph option is set. Please set one.");
//            }

            if (list) {
                validateConnectionSettings(parser);
                doList(parser);
            }

            if (jmx) {
                validateConnectionSettings(parser);
                doJmx(parser);
            }
            if (graph) {
               doGraphing(parser);
            }
        } catch (Exception e) {
            throw e;
            // TODO MVR
//            LOG.error("An exception occured", e);
//            System.err.println("JmxConfigGenerator [options...] arguments...");
//            parser.printUsage(System.err);
//            System.err.println();
//            System.err.println("Examples:");
//            System.err.println(" Generation of jmx-datacollection.xml: java -jar JmxConfigGenerator.jar -jmx -host localhost -port 7199 -out JMX-DatacollectionDummy.xml [-service cassandra] [-skipDefaultVM] [-runWritableMBeans] [-dictionary dictionary.properties]");
//            System.err.println(" Generation of snmp-graph.properties: java -jar JmxConfigGenerator.jar -graph -input test.xml -out test.properies [-template graphTemplate.vm] [-service cassandra]");
        }
    }

    private void validateConnectionSettings(final CmdLineParser parser) throws CmdLineException {
        if (url != null && (hostName != null ||port != null)) {
            LOG.warn("You have defined url and a hostname and/or port. Using url and ignoring hostname:port");
        }
        if (url == null && (hostName == null || port == null)) {
            throw new CmdLineException(parser, "You have to define either an url or an hostname and port to connect to a jmx server.");
        }
    }

    private void doList(final CmdLineParser parser) throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        try (JMXConnector connector = getJmxConnector()) {
            MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();


            QueryDescriptor queryDescriptor = idFilter == null ? new QueryDescriptor() : QueryDescriptor.parse(idFilter);
            ObjectName objectName = queryDescriptor.objectName != null ? new ObjectName(queryDescriptor.objectName) : null;

            Set<ObjectInstance> objectInstances = mBeanServerConnection.queryMBeans(objectName, null);
            ArrayList<ObjectInstance> sortedObjectInstances = new ArrayList<ObjectInstance>(objectInstances);
            Collections.sort(sortedObjectInstances, new Comparator<ObjectInstance>() {

                @Override
                public int compare(ObjectInstance o1, ObjectInstance o2) {
                    return o1.getObjectName().compareTo(o2.getObjectName());
                }
            });

            int i = 1;
            Pattern pattern = queryDescriptor.attributeName != null ? Pattern.compile(queryDescriptor.attributeName) : null;
            for (ObjectInstance eachInstance : sortedObjectInstances) {
                System.out.println(String.format("% 5d\t%s", i, eachInstance.getObjectName()));

                MBeanInfo mBeanInfo = mBeanServerConnection.getMBeanInfo(eachInstance.getObjectName());
                int a = 1;
                for (MBeanAttributeInfo eachAttribute : mBeanInfo.getAttributes()) {
                    if (pattern == null || pattern.matcher(eachAttribute.getName()).matches()) {
                        System.out.println(String.format("     \t% 5d\t%s", a, eachAttribute.getName()));
                        a++;
                    }
                }
                i++;
            }

            if (idFilter != null) {
                System.out.println(String.format("Your query '%s' shows %d/%d MBeans.", queryDescriptor.toString(), i-1, mBeanServerConnection.getMBeanCount()));
            } else {
                System.out.println(String.format("There are %d registered MBeans", mBeanServerConnection.getMBeanCount()));
            }
        }
    }

    private void doGraphing(final CmdLineParser parser) throws IOException, CmdLineException {
        if (inputFile == null) {
            throw new CmdLineException(parser, "You have not specified an input file.");
        }
        if (outFile == null) {
            throw new CmdLineException(parser, "You have not specified an output file.");
        }
        if (!Files.exists(Paths.get(inputFile))) {
            throw new CmdLineException(parser, "You have specified an input file which does not exist.");
        }

        JmxConfigReader jmxToSnmpGraphConfigGen = new JmxConfigReader();
        Collection<Report> reports = jmxToSnmpGraphConfigGen.generateReportsByJmxDatacollectionConfig(inputFile);

        GraphConfigGenerator graphConfigGenerator = new GraphConfigGenerator();

        String snmpGraphConfig;
        if (templateFile != null) {
            snmpGraphConfig = graphConfigGenerator.generateSnmpGraph(reports, templateFile);
        } else {
            snmpGraphConfig = graphConfigGenerator.generateSnmpGraph(reports);
        }

        System.out.println(snmpGraphConfig);
        FileUtils.writeStringToFile(new File(outFile), snmpGraphConfig, "UTF-8");
    }

    private JMXServiceURL getServiceUrl() throws MalformedURLException {
        JMXServiceURL jmxServiceURL = null;
        if (url != null) {
            return new JMXServiceURL(url);
        }
        if (hostName != null && port != null) {
            return getJmxServiceURL(jmxmp, hostName, port);
        }
        // TODO MVR ...
        throw new RuntimeException("bla");
    }


    /**
     * determines the jmxServiceUrl depending on jmxmp.
     *
     * @param jmxmp
     * @param hostname
     * @param port
     * @return
     * @throws MalformedURLException
     */
    private static JMXServiceURL getJmxServiceURL(boolean jmxmp, String hostname, String port) throws MalformedURLException {
        if (jmxmp) {
            return new JMXServiceURL(String.format("service:jmx:jmxmp://%s:%s", hostname, port));
        }
        return new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", hostname, port));
    }

    /**
     * This method gets the JmxConnector to connect with the given
     * jmxServiceURL.
     *
     * @param username
     *            may be null
     * @param password
     *            may be null
     * @param jmxServiceURL
     *            should not be null!
     * @return a jmxConnector
     * @throws IOException
     *             if the connection to the given jmxServiceURL fails (e.g.
     *             authentication failure or not reachable)
     */
    private JMXConnector getJmxConnector() throws IOException {
        HashMap<String, String[]> env = new HashMap<String, String[]>();
        if (username != null && password != null) {
            String[] credentials = new String[] { username, password };
            env.put("jmx.remote.credentials", credentials);
        }
        JMXConnector jmxConnector = JMXConnectorFactory.connect(getServiceUrl(), env);
        return jmxConnector;
    }

    private void doJmx(CmdLineParser parser) throws IOException, CmdLineException {
        JmxDatacollectionConfiggenerator jmxConfigGenerator = new JmxDatacollectionConfiggenerator();
        Map<String, String> dictionary = loadInternalDictionary();
        if (dictionaryFile != null) {
            dictionary = loadExternalDictionary(dictionaryFile);
        }
        try (JMXConnector jmxConnector = getJmxConnector()) {
            MBeanServerConnection mBeanServerConnection = jmxConfigGenerator.createMBeanServerConnection(jmxConnector);
            JmxDatacollectionConfig generateJmxConfigModel = jmxConfigGenerator.generateJmxConfigModel(mBeanServerConnection, serviceName, !skipDefaultVM, runWritableMBeans, dictionary);
            jmxConfigGenerator.writeJmxConfigFile(generateJmxConfigModel, outFile);
        }
    }

    // TODO make this private!
    public static Map<String, String> loadInternalDictionary() {
        Map<String, String> internalDictionary = new HashMap<String, String>();
        Properties properties = new Properties();
        try {
            BufferedInputStream stream = new BufferedInputStream(Starter.class.getClassLoader().getResourceAsStream("dictionary.properties"));
            properties.load(stream);
            stream.close();
        } catch (IOException ex) {
            LOG.error("Load dictionary entries from internal properties files error: '{}'", ex.getMessage());
        }
        LOG.info("Loaded '{}' internal dictionary entries", properties.size());
        for (final Entry<?,?> entry : properties.entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            internalDictionary.put(key.toString(), value == null? null : value.toString());
        }
        LOG.info("Dictionary entries loaded: '{}'", internalDictionary.size());
        return internalDictionary;
    }
    
    private Map<String, String> loadExternalDictionary(String dictionaryFile) {
        Map<String, String> externalDictionary = new HashMap<String, String>();
        Properties properties = new Properties();
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(dictionaryFile));
            properties.load(stream);
            stream.close();
        } catch (FileNotFoundException ex) {
            LOG.error("'{}'", ex.getMessage());
        } catch (IOException ex) {
            LOG.error("'{}'", ex.getMessage());
        }
        LOG.info("Loaded '{}' external dictionary entries from '{}'", properties.size(), dictionaryFile);
        for (final Entry<?,?> entry : properties.entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            externalDictionary.put(key.toString(), value == null? null : value.toString());
        }
        LOG.info("Dictionary entries loaded: '{}'", externalDictionary.size());
        return externalDictionary;
	}
}
