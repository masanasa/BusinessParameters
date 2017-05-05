package be.kwakeroni.scratch;

import be.kwakeroni.parameters.backend.api.factory.BusinessParametersBackendFactory;
import be.kwakeroni.parameters.backend.inmemory.api.GroupData;
import be.kwakeroni.parameters.backend.inmemory.api.InMemoryGroup;
import be.kwakeroni.parameters.backend.inmemory.factory.InMemoryBackendServiceFactory;
import be.kwakeroni.parameters.backend.inmemory.service.InMemoryBackend;
import be.kwakeroni.parameters.basic.definition.factory.MappedDefinitionVisitor;
import be.kwakeroni.parameters.basic.definition.factory.RangedDefinitionVisitor;
import be.kwakeroni.parameters.basic.definition.factory.SimpleDefinitionVisitor;
import be.kwakeroni.parameters.basic.definition.inmemory.InMemoryMappedGroupFactory;
import be.kwakeroni.parameters.basic.definition.inmemory.InMemoryRangedGroupFactory;
import be.kwakeroni.parameters.basic.definition.inmemory.InMemorySimpleGroupFactory;
import be.kwakeroni.parameters.client.api.model.ParameterGroup;
import be.kwakeroni.parameters.definition.api.DefinitionVisitorContext;
import be.kwakeroni.scratch.tv.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * (C) 2017 Maarten Van Puymbroeck
 */
public class InMemoryTestData implements TestData {

    private final InMemoryBackend backend;
    private final List<String> groups = new ArrayList<>();

    public InMemoryTestData() {
        this.backend = InMemoryBackendServiceFactory.getSingletonInstance();
        reset();
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public boolean acceptBackend(BusinessParametersBackendFactory factory) {
        return factory instanceof InMemoryBackendServiceFactory;
    }

    @Override
    public boolean hasDataForGroup(String name) {
        return this.groups.contains(name);
    }

    private <G extends ParameterGroup<?>> void setGroupData(G group1, G group2, Function<G, GroupData> data) {
        setGroupData(group1.getName(), data.apply(group1));
        setGroupData(group2.getName(), data.apply(group2));
    }

    private void setGroupData(String groupName, GroupData data) {
        this.backend.setGroupData(groupName, data);
        this.groups.add(groupName);
    }

    @Override
    public void notifyModifiedGroup(String name) {

    }

    @Override
    public void reset() {
        this.groups.clear();

        setGroupData(SimpleTVGroup.instance().getName(),
                SimpleTVGroup.getData(Dag.MAANDAG, Slot.atHour(20)));

        setGroupData(MappedTVGroup.instance().getName(),
                MappedTVGroup.getData(Dag.ZATERDAG, "Samson",
                        Dag.ZONDAG, "Morgen Maandag"));

        setGroupData(RangedFilterTVGroup.instance(), RangedQueryTVGroup.instance(),
                group -> AbstractRangedTVGroup.getData(group, Slot.atHour(8), Slot.atHour(12), "Samson",
                        Slot.atHalfPast(20), Slot.atHour(22), "Morgen Maandag"));

        setGroupData(MappedRangedFilterTVGroup.instance(), MappedRangedQueryTVGroup.instance(),
                group -> AbstractMappedRangedTVGroup.getData(group.getName(),
                        AbstractMappedRangedTVGroup.entryData(Dag.MAANDAG, Slot.atHalfPast(20), Slot.atHour(22), "Gisteren Zondag"),
                        AbstractMappedRangedTVGroup.entryData(Dag.ZATERDAG, Slot.atHour(8), Slot.atHour(12), "Samson"),
                        AbstractMappedRangedTVGroup.entryData(Dag.ZATERDAG, Slot.atHour(14), Slot.atHour(18), "Koers"),
                        AbstractMappedRangedTVGroup.entryData(Dag.ZONDAG, Slot.atHalfPast(20), Slot.atHour(22), "Morgen Maandag")
                ));
    }

    public static DefinitionVisitorContext<InMemoryGroup> FACTORY_CONTEXT = Contexts.of(
            SimpleDefinitionVisitor.class, new InMemorySimpleGroupFactory(),
            MappedDefinitionVisitor.class, new InMemoryMappedGroupFactory(),
            RangedDefinitionVisitor.class, new InMemoryRangedGroupFactory()
    );

}
