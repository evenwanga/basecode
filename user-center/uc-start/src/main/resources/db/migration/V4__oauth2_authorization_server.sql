-- Spring Authorization Server 表结构

CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id varchar(100) PRIMARY KEY,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret varchar(200),
    client_secret_expires_at timestamp,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000),
    post_logout_redirect_uris varchar(1000),
    scopes varchar(1000),
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth2_client_id ON oauth2_registered_client (client_id);

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id varchar(100) PRIMARY KEY,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000),
    attributes text,
    state varchar(500),
    authorization_code_value bytea,
    authorization_code_issued_at timestamp,
    authorization_code_expires_at timestamp,
    authorization_code_metadata text,
    access_token_value bytea,
    access_token_issued_at timestamp,
    access_token_expires_at timestamp,
    access_token_metadata text,
    access_token_type varchar(100),
    access_token_scopes varchar(1000),
    oidc_id_token_value bytea,
    oidc_id_token_issued_at timestamp,
    oidc_id_token_expires_at timestamp,
    oidc_id_token_metadata text,
    refresh_token_value bytea,
    refresh_token_issued_at timestamp,
    refresh_token_expires_at timestamp,
    refresh_token_metadata text,
    user_code_value bytea,
    user_code_issued_at timestamp,
    user_code_expires_at timestamp,
    user_code_metadata text,
    device_code_value bytea,
    device_code_issued_at timestamp,
    device_code_expires_at timestamp,
    device_code_metadata text
);
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_registered_client ON oauth2_authorization (registered_client_id);
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_principal_name ON oauth2_authorization (principal_name);
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_token ON oauth2_authorization (state, authorization_code_value, access_token_value, refresh_token_value, device_code_value, user_code_value);
