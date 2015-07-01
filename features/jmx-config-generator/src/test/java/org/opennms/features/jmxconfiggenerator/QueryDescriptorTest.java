package org.opennms.features.jmxconfiggenerator;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by mvrueden on 01/07/15.
 */
public class QueryDescriptorTest {

    @Test
    public void testParsing() {
        validateAttributeNameIsNull("org.eclipse.jetty.server.session:type=sessionhandler,context=opennms-remoting,id=0");
        validateAttributeNameIsNull("org.eclipse.jetty.servlet:type=filtermapping,name=springSecurityFilterChain,id=1");
        validateAttributeNameIsNull("org.eclipse.jetty.servlet:type=servletmapping,name=__org.eclipse.jetty.servlet.JspPropertyGroupServlet__,id=0");
        validateAttributeNameIsNull("org.eclipse.jetty.servlet:type=servletmapping,name=snmpGetInterfaces,id=0");
        validateAttributeNameIsNull("org.eclipse.jetty.servlet:type=servletmapping,name=dispatcher,id=8");

        validateFullParsing("org.eclipse.jetty.servlet:type=filtermapping,name=springSecurityFilterChain,id=1", "abc");
        validateFullParsing("org.eclipse.jetty.server.session:type=sessionhandler,context=opennms-remoting,id=0", "running");
        validateFullParsing("org.eclipse.jetty.servlet:type=servletmapping,name=__org.eclipse.jetty.servlet.JspPropertyGroupServlet__,id=0","PendingState");

    }

    private void validateFullParsing(String objectname, String attributeName) {
        QueryDescriptor qd = QueryDescriptor.parse(attributeName != null ?  objectname + ":" + attributeName : objectname);
        Assert.assertEquals(attributeName, qd.attributeName);
        Assert.assertEquals(objectname, qd.objectName);
    }

    private void validateAttributeNameIsNull(String input) {
        validateFullParsing(input, null);
    }
}
