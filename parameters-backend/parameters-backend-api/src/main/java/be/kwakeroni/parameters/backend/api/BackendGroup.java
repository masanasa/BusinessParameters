package be.kwakeroni.parameters.backend.api;

import be.kwakeroni.parameters.backend.api.query.BackendQuery;
import be.kwakeroni.parameters.backend.api.query.BackendWireFormatterContext;
import be.kwakeroni.parameters.definition.api.ParameterGroupDefinition;

/**
 * (C) 2016 Maarten Van Puymbroeck
 */
public interface BackendGroup<Q> {

    public default <ES extends BackendGroup<Q>> ES as(Class<? super ES> type) {
        return (ES) type.cast(this);
    }

    public String getName();
    public ParameterGroupDefinition getDefinition();

    public BackendQuery<? extends Q, ?> internalize(Object query, BackendWireFormatterContext context);

}
