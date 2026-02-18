package io.eel.common.model;

import java.util.Objects;
import java.util.UUID;

public class DataSource {

    private UUID id;

    private String name;

    private String secretId;

    public DataSource() {}

    public DataSource(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecretId() {
        return this.secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSource that = (DataSource) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(secretId, that.secretId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, secretId);
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", secretId='" + secretId + '\'' +
                '}';
    }

}
