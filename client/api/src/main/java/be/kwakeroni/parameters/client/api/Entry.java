package be.kwakeroni.parameters.client.api;

import be.kwakeroni.parameters.client.model.Parameter;

public interface Entry {

    <T> T getValue(Parameter<T> parameter);

}