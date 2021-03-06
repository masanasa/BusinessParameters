package be.kwakeroni.parameters.adapter.jmx.api;

import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.DescriptorSupport;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by kwakeroni on 10/05/17.
 */
class DefaultJMXGroupBuilder implements JMXGroupBuilder {

    private final String name;
    private Map<String, JMXOperationBuilder> operations = new HashMap<>();

    DefaultJMXGroupBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public String getGroupName() {
        return this.name;
    }

    @Override
    public JMXGroupBuilder withParameters(Collection<String> parameters) {
        return this;
    }

    @Override
    public JMXOperationBuilder addQuery(String name) {
        return operations.merge(name, JMXOperationBuilder.newQuery(),
                ($, $$) -> {
                    throw new IllegalStateException("Operation " + name + " already defined in JMX Group MBean " + name);
                });
    }

    @Override
    public JMXOperationBuilder getQuery(String name) {
        JMXOperationBuilder operation = operations.get(name);
        if (operation == null) {
            throw new IllegalStateException("No operation " + name + " defined in JMX Group MBean " + name);
        }
        return operation;
    }

    @Override
    public MBeanInfo getMBeanInfo(Class<?> targetClass) {

        final Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("name", this.name);
        descriptor.setField("descriptorType", "mbean");

        return new MBeanInfo(
                targetClass.getName(),
                "Provides access to parameters of group " + name,
                null,      // attributes
                null,      // constructors
                operations.values().stream().map(JMXOperationBuilder::toOperationInfo).toArray(MBeanOperationInfo[]::new),
                null,      // notifications
                descriptor);

    }

    @Override
    public Map<String, JMXOperation> getOperationsByName() {
        return this.operations.values().stream().collect(Collectors.toMap(JMXOperationBuilder::getName, JMXOperationBuilder::toOperation));
    }
}
