package be.kwakeroni.parameters.api.backend;

import java.util.Collection;

/**
 * (C) 2016 Maarten Van Puymbroeck
 */
public interface BusinessParametersBackend {

    public Object get(String group, Object query);

    public Collection<String> getGroupNames();
}
