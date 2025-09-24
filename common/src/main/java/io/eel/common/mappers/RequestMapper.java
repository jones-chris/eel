package io.eel.common.mappers;

import io.eel.common.http.HttpRequest;

public interface RequestMapper<T> {

    HttpRequest map(T obj);

}
