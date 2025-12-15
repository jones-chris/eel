package io.eel.common.dao;

import io.eel.common.model.SqlDataSourceSecret;

public interface SqlDataSourceSecretDao {

    SqlDataSourceSecret getById(String secretId);

}
