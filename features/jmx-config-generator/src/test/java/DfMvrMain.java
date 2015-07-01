import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DfMvrMain {

    public static void main(String[] args) throws IOException, IntrospectionException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException, AttributeNotFoundException, MBeanException {
        final Map<String, Object> env = new HashMap<>();
//        env.put("jmx.remote.credentials", new String[]{"cassandra", "cassandrapassword"});
//        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://10.10.10.10:7199/jmxrmi");

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:18980/jmxrmi");



        try (JMXConnector connection = JMXConnectorFactory.connect(url, env)) {
            MBeanServerConnection mBeanServerConnection = connection.getMBeanServerConnection();
            System.out.println("MBean count: " + mBeanServerConnection.getMBeanCount());

            ObjectName objectName = new ObjectName("org.eclipse.jetty.server:*"); //:name=starting,*"); //osgi.core:version=1.7,*"); //, "type", "serviceState"); //:type=serviceState,version=1.7,framework=org.apache.felix.framework,uuid=2ed9b71b-74dd-4cbe-b239-aa71e3545980");

            QueryExp queryExp = Query.eq(
                    Query.attr("starting"),
                    Query.value(false));
            System.out.println(queryExp.toString());

            Set < ObjectInstance > objects = mBeanServerConnection.queryMBeans(objectName, queryExp);
            List<ObjectInstance> objectList = new ArrayList<>(objects);
            Collections.sort(objectList, new Comparator<ObjectInstance>() {

                @Override
                public int compare(ObjectInstance o1, ObjectInstance o2) {
                    return o1.getObjectName().compareTo(o2.getObjectName());
                }
            });

            Object starting = mBeanServerConnection.getAttribute(objectName, "starting");
            //System.out.println(objectName);
            //System.out.println(starting);

            Set<String> typeSet = new HashSet<>();
            int i = 1;
            //System.exit(0);
            for (ObjectInstance eachInstance : objectList) {
                System.out.println(i + ": " + eachInstance.getObjectName());
                MBeanInfo mbeanInfo = mBeanServerConnection.getMBeanInfo(eachInstance.getObjectName());
                MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
                System.out.println("\t Attributes count: " + attributes.length);
                int a = 1;
                for (MBeanAttributeInfo eachAttribute : attributes) {
                    typeSet.add(eachAttribute.getType());
                    System.out.println("\t " + a + ": " + eachAttribute.toString());
                    a++;
                }
                i++;
            }
            System.out.println();
            System.out.println("Types:");
            for (String eachType : typeSet) {
                System.out.println(eachType);
            }
        }
    }
}
