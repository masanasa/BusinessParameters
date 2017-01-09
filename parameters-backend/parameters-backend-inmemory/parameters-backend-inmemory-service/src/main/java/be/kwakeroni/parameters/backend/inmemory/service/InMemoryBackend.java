package be.kwakeroni.parameters.backend.inmemory.service;

import be.kwakeroni.parameters.api.backend.BusinessParametersBackend;
import be.kwakeroni.parameters.api.backend.query.InternalizationContext;
import be.kwakeroni.parameters.api.backend.query.Internalizer;
import be.kwakeroni.parameters.backend.inmemory.api.DataQuery;
import be.kwakeroni.parameters.backend.inmemory.api.GroupData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * (C) 2016 Maarten Van Puymbroeck
 */
public class InMemoryBackend implements BusinessParametersBackend {

    Logger LOG = LoggerFactory.getLogger(InMemoryBackend.class);

    private final Map<String, GroupData> data;
    private final InternalizationContext<DataQuery<?>> internalizationContext;

    public InMemoryBackend(InternalizationContext<DataQuery<?>> context) {
        this(new HashMap<>(), context);
    }

    private InMemoryBackend(Map<String, GroupData> data, InternalizationContext<DataQuery<?>> context) {
        this.data = data;
        this.internalizationContext = context;
    }

    public void setGroupData(String groupName, GroupData data){
        this.data.put(groupName, data);
    }

    public void addGroupData(String groupName, GroupData data) {
        this.data.merge(groupName, data, (key, d) -> {
            throw new IllegalStateException("Duplicate data for group " + key);
        });
    }

    @Override
    public Object get(String group, Object queryObject) {
        GroupData groupData = getGroupData(group);
        System.out.println("Internalizing query {}" + queryObject);
        DataQuery<?> query = internalizationContext.internalize(groupData.getGroup(), queryObject);
        Object result = getExternalResult(query, group, groupData);
        System.out.println("Returning result {}" + result + " for query {}" + query);
        return result;
    }

    private <T> Object getExternalResult(DataQuery<T> query, String group, GroupData groupData){
        System.out.println("Executing query {}" + query +" on {}" + group);
        T result = query.apply(groupData.getEntries()).orElse(null);
        System.out.println("Query on {}" + group + " has result {}" + result);
        System.out.println("Externalizing result {}" + result);
        Object external = query.externalizeResult(result, this.internalizationContext);
        return external;
    }

    private GroupData getGroupData(String name) {
        return Optional.ofNullable(data.get(name))
                .orElseThrow(() -> new IllegalArgumentException("No group defined with name " + name));
    }

    @Override
    public Collection<String> getGroupNames() {
        return data.keySet();
    }
}
