package be.kwakeroni.parameters.basic.client.model;

import be.kwakeroni.parameters.client.api.model.Parameter;

/**
 * (C) 2016 Maarten Van Puymbroeck
 */
public interface Entry {

    <T> T getValue(Parameter<T> parameter);

}