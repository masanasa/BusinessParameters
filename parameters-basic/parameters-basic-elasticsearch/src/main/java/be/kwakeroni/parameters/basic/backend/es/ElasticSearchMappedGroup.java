package be.kwakeroni.parameters.basic.backend.es;

import be.kwakeroni.parameters.backend.es.api.*;
import be.kwakeroni.parameters.basic.backend.query.MappedBackendGroup;
import be.kwakeroni.parameters.basic.backend.query.support.IntermediaryBackendGroupSupport;
import be.kwakeroni.parameters.basic.backend.query.support.IntermediateBackendQuerySupport;
import be.kwakeroni.parameters.definition.api.ParameterGroupDefinition;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * (C) 2017 Maarten Van Puymbroeck
 */
public class ElasticSearchMappedGroup
        extends IntermediaryBackendGroupSupport<ElasticSearchQuery<?>, ElasticSearchGroup>
        implements ElasticSearchGroup, MappedBackendGroup<ElasticSearchQuery<?>, ElasticSearchGroup> {

    private final String keyParameterName;

    public ElasticSearchMappedGroup(String keyParameterName, ParameterGroupDefinition definition, ElasticSearchGroup subGroup) {
        super(definition, subGroup);
        this.keyParameterName = keyParameterName;
    }

    @Override
    public ElasticSearchQuery<?> getEntryQuery(String keyValue, ElasticSearchQuery<?> subQuery) {
        return new ElasticSearchMappedQuery<>(keyValue, subQuery);
    }

    @Override
    public ElasticSearchEntry prepareAndValidateNewEntry(ElasticSearchEntry entry, ElasticSearchData storage) {
        String keyValue = entry.getParameter(this.keyParameterName);

        try {
            return getSubGroup().prepareAndValidateNewEntry(entry, storage.with(mapKeyFilter(keyValue)));
        } catch (IllegalStateException exc) {
            throw new IllegalStateException(exc.getMessage() + " with key: " + this.keyParameterName + "=" + keyValue);
        }
    }

    @Override
    public String toString() {
        return "mapped(" + this.keyParameterName + " : " + getSubGroup() + ")";
    }


    private Consumer<ElasticSearchCriteria> mapKeyFilter(String keyValue) {
        return criteria -> criteria.addParameterMatch(keyParameterName, keyValue);
    }

    private final class ElasticSearchMappedQuery<T>
            extends IntermediateBackendQuerySupport<ElasticSearchQuery<T>, T>
            implements ElasticSearchQuery<T> {

        private final String keyValue;

        public ElasticSearchMappedQuery(String keyValue, ElasticSearchQuery<T> subQuery) {
            super(subQuery);
            this.keyValue = keyValue;
        }

        @Override
        public Optional<T> apply(ElasticSearchData data) {
            return getSubQuery().apply(data.with(mapKeyFilter(this.keyValue)));
        }

        @Override
        public EntryModification getEntryModification(T value, ElasticSearchData data) {
            return getSubQuery().getEntryModification(value, data.with(mapKeyFilter(this.keyValue)));
        }
    }
}
