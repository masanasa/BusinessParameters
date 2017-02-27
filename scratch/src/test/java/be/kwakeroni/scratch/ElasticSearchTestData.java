package be.kwakeroni.scratch;

import be.kwakeroni.parameters.backend.api.BackendGroup;
import be.kwakeroni.parameters.backend.es.api.ElasticSearchQuery;
import be.kwakeroni.parameters.backend.es.factory.ElasticSearchBackendServiceFactory;
import be.kwakeroni.parameters.backend.es.service.ElasticSearchBackend;
import be.kwakeroni.parameters.backend.inmemory.api.EntryData;
import be.kwakeroni.scratch.tv.Dag;
import be.kwakeroni.scratch.tv.MappedRangedTVGroup;
import be.kwakeroni.scratch.tv.MappedTVGroup;
import be.kwakeroni.scratch.tv.RangedTVGroup;
import be.kwakeroni.scratch.tv.SimpleTVGroup;
import be.kwakeroni.scratch.tv.Slot;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * (C) 2017 Maarten Van Puymbroeck
 */
public class ElasticSearchTestData implements TestData {
    private static Logger LOG = org.slf4j.LoggerFactory.getLogger(ElasticSearchTestData.class);

    private final ElasticSearchTestNode elasticSearch;
    private final ElasticSearchBackend backend;
    private final Client client = new Client();
    private final List<String> groups = new ArrayList<>();


    public ElasticSearchTestData() {
        this.backend = ElasticSearchBackendServiceFactory.getSingletonInstance();
        this.elasticSearch = ElasticSearchTestNode.start();
        this.elasticSearch.waitUntilStarted();
        reset();
    }

    @Override
    public void close() throws Exception {
        this.elasticSearch.close();
    }

    @Override
    public void reset() {
        this.groups.clear();
        callES("/parameters", WebResource::delete);

        LOG.info("Inserting test data...");

        register(SimpleTVGroup.ELASTICSEARCH_GROUP);
        insert(SimpleTVGroup.instance().getName(), SimpleTVGroup.getEntryData(Dag.MAANDAG, Slot.atHour(20)));

        register(MappedTVGroup.ELASTICSEARCH_GROUP);
        insert(MappedTVGroup.instance().getName(), MappedTVGroup.entryData(Dag.ZATERDAG, "Samson"));
        insert(MappedTVGroup.instance().getName(), MappedTVGroup.entryData(Dag.ZONDAG, "Morgen Maandag"));

        register(RangedTVGroup.ELASTICSEARCH_GROUP_WITH_POSTFILTER);
        insert(RangedTVGroup.instance().getName(),
                RangedTVGroup.entryData(Slot.atHour(8), Slot.atHour(12), "Samson"),
                RangedTVGroup.entryData(Slot.atHalfPast(20), Slot.atHour(22), "Morgen Maandag"));

        register(MappedRangedTVGroup.ELASTICSEARCH_POSTFILTER_GROUP);
        String uuid = insert(MappedRangedTVGroup.instance().getName(),
                MappedRangedTVGroup.entryData(Dag.MAANDAG, Slot.atHalfPast(20), Slot.atHour(22), "Gisteren Zondag"),
                MappedRangedTVGroup.entryData(Dag.ZATERDAG, Slot.atHour(8), Slot.atHour(12), "Samson"),
                MappedRangedTVGroup.entryData(Dag.ZATERDAG, Slot.atHour(14), Slot.atHour(18), "Koers"),
                MappedRangedTVGroup.entryData(Dag.ZONDAG, Slot.atHalfPast(20), Slot.atHour(22), "Morgen Maandag")
        );

        LOG.info("Waiting for test data to become available...");

        try {
            this.elasticSearch.waitUntil(() -> get(MappedRangedTVGroup.instance().getName(), uuid) != null, 5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasDataForGroup(String name) {
        return groups.contains(name);
    }

    private void register(BackendGroup<ElasticSearchQuery<?>, ?, ?> group) {
        backend.registerGroup(group);
        this.groups.add(group.getName());
    }

    private String get(String group, String uuid) {
        String url = String.format("/parameters/%s/%s", group, uuid);
        ClientResponse response = callES(url, WebResource::get);
        int status = response.getStatus();
        if (status == 204 || status == 404) {
            return null;
        } else if (status < 400) {
            return response.getEntity(String.class);
        } else {
            throw new RuntimeException(status + " - " + response.getEntity(String.class));
        }

    }

    private String insert(String group, EntryData... entryDatas) {
        String uuid = null;
        for (EntryData entryData : entryDatas) {
            uuid = insert(group, entryData);
        }
        return uuid;
    }

    private String insert(String group, EntryData entryData) {
        String uuid = UUID.randomUUID().toString();
        String url = String.format("/parameters/%s/%s", group, uuid);
        callES(url, toJson(entryData), WebResource::put);
        return uuid;
    }

    private void callES(String path, String body, CallWithBody call) {
        String url = resolve("http://127.0.0.1:9200", path);
        LOG.info("{} : {}\r\n{}", url, call, body);
        ClientResponse response = call.call(client.resource(url), body);
        LOG.info("[{}] {}\r\n{}", response.getStatus(), url, response.getEntity(String.class));
    }

    private ClientResponse callES(String path, CallWithoutBody call) {
        String url = resolve("http://127.0.0.1:9200", path);
        LOG.info("{} : {}", url, call);
        ClientResponse response = call.call(client.resource(url));
        byte[] bytes = response.getEntity(byte[].class);
        response.setEntityInputStream(new ByteArrayInputStream(bytes));
        LOG.info("[{}] {}\r\n{}", response.getStatus(), url, new String(bytes));
        return response;
    }

    private String resolve(String base, String path) {
        return base + ((path.startsWith("/")) ? "" : "/") + path + "?pretty";
    }

    private String toJson(EntryData entry) {
        return new JSONObject(entry.asMap()).toString(4);
    }

    private static interface CallWithoutBody {
        <T> T call(WebResource resource, Class<T> type);

        default ClientResponse call(WebResource resource) {
            return call(resource, ClientResponse.class);
        }
    }

    private static interface CallWithBody {
        <T> T call(WebResource resource, Class<T> type, String body);

        default ClientResponse call(WebResource resource, String body) {
            return call(resource, ClientResponse.class, body);
        }
    }

}
// INFO  - http://127.0.0.1:9200/13591bee-a2f9-431c-ab27-3572bf5aeb4f?pretty
//  http://127.0.0.1:9200/parameters/tv.simple/13591bee-a2f9-431c-ab27-3572bf5aeb4f?pretty