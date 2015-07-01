package org.opennms.features.jmxconfiggenerator;

/**
 * Created by mvrueden on 01/07/15.
 */
public class QueryDescriptor {
    public String objectName;
    public String attributeName;

    public static QueryDescriptor parse(String input) {
        QueryDescriptor qd = new QueryDescriptor();
        String[] split = input.split(":");
        if (split.length == 3) {
            qd.objectName = input.substring(0, input.lastIndexOf(":"));
            qd.attributeName = input.substring(input.lastIndexOf(":") + 1);
        } else {
            qd.objectName = input;
        }
        return qd;
    }

    public String toString() {
        if (attributeName != null) {
            return objectName + ":" + attributeName;
        }
        return objectName;
    }
}
