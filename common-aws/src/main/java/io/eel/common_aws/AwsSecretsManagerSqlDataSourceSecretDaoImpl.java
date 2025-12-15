package io.eel.common_aws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.dao.SqlDataSourceSecretDao;
import io.eel.common.model.SqlDataSourceSecret;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.util.logging.Logger;

public class AwsSecretsManagerSqlDataSourceSecretDaoImpl implements SqlDataSourceSecretDao {

    private final static Logger log = Logger.getLogger(AwsSecretsManagerSqlDataSourceSecretDaoImpl.class.getName());

    private final static String UNEXPECTED_SECRET_FORMAT_ERROR_MESSAGE = "Secrets Manager response is not a secret string as expected for secret id %s";

    private final static Gson gson = new GsonBuilder().create();

    private SecretsManagerClient secretsManagerClient;

    private AwsSecretsManagerSqlDataSourceSecretDaoImpl() {}

    public AwsSecretsManagerSqlDataSourceSecretDaoImpl(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Override
    public SqlDataSourceSecret getById(String secretId) {
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretId)
                    .build();

            GetSecretValueResponse valueResponse = this.secretsManagerClient.getSecretValue(valueRequest);

            if (valueResponse.secretString() != null) {
                return gson.fromJson(valueResponse.secretString(), SqlDataSourceSecret.class);
            }

            final String errorMessage = UNEXPECTED_SECRET_FORMAT_ERROR_MESSAGE.formatted(secretId);
            log.severe(errorMessage);
            throw new RuntimeException(errorMessage);
        } catch (SecretsManagerException e) {
            log.severe("Error when retrieving and deserializing the secret " + secretId + ", error: " + e.getMessage());
            throw e;
        }
    }

}
