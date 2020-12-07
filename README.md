# Instructions

## Server without SSL
```
cd server;mvn spring-boot:run
```
## CURL with SSL
 curl -i  -v -XGET http://localhost:8080/saas-gw-ms/api-1
 
## Create Server Side Truststore 

To create a keystore with a public and private key, execute the following command in your terminal:

```
keytool -genkeypair -keyalg RSA -keysize 2048 -alias server -dname "CN=Hakan,OU=Amsterdam,O=Thunderberry,C=NL" -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -validity 3650 -keystore server/src/main/resources/identity.jks -storepass secret -keypass secret -deststoretype pkcs12
```

## Check SSL Connection
curl -i --insecure -v -XGET https://localhost:8443/saas-gw-ms/api-1

## Create Unsigned Server Side Certificate

```
keytool -exportcert -keystore server/src/main/resources/identity.jks -storepass secret -alias server -rfc -file server/src/main/resources/server.cer
```

## Create Client Side Keystore
```
keytool -genkeypair -keyalg RSA -keysize 2048 -alias client -dname "CN=Suleyman,OU=Altindag,O=Altindag,C=NL" -validity 3650 -keystore client/src/main/resources/identity.jks -storepass secret -keypass secret -deststoretype pkcs12
```

## Generate Client Side Unsigned Certificate
```
keytool -exportcert -keystore client/src/main/resources/identity.jks -storepass secret -alias client -rfc -file client/src/main/resources/client.cer
```


## Creating a Certificate Authority

```
keytool -genkeypair -keyalg RSA -keysize 2048 -alias root-ca -dname "CN=Root-CA,OU=Certificate Authority,O=Thunderberry,C=NL" -validity 3650 -ext bc:c -keystore root-ca/identity.jks -storepass secret -keypass secret -deststoretype pkcs12
```

## Certificate Signing Request for the server

```
keytool -certreq -keystore server/src/main/resources/identity.jks -alias server -keypass secret -storepass secret -keyalg rsa -file server/src/main/resources/server.csr
```

## Certificate Signing Request for the client

```
keytool -certreq -keystore client/src/main/resources/identity.jks -alias client -keypass secret -storepass secret -keyalg rsa -file client/src/main/resources/client.csr
```

The Certificate Authority need these csr files to be able to sign it. The next step will be signing the requests.

## Signing the certificate with the Certificate Signing Request

## Generate CA Cert file
```
keytool -exportcert -keystore root-ca/identity.jks -storepass secret -alias root-ca -rfc -file root-ca/ca.cer
```

## Convert java keystore to a p12 file
```
keytool -importkeystore -srckeystore root-ca/identity.jks -destkeystore root-ca/root-ca.p12 -srcstoretype jks -deststoretype pkcs12 -srcstorepass secret -deststorepass secret
```

## Create pem file from a p12 file
```
openssl pkcs12 -in root-ca/root-ca.p12 -out root-ca/root-ca.pem -nokeys -passin pass:secret -passout pass:secret
```

## Create a key file from a p12 file
```
openssl pkcs12 -in root-ca/root-ca.p12 -out root-ca/root-ca.key -nocerts -passin pass:secret -passout pass:secret
```

The next step will be signing the certificates. You can sign it with the following commands:

## Signing the client certificate
```
openssl x509 -req -in client/src/main/resources/client.csr -CA root-ca/root-ca.pem -CAkey root-ca/root-ca.key -CAcreateserial -out client/src/main/resources/client-signed.cer -days 1825 -passin pass:secret
```

## Signing the server certificate
```
openssl x509 -req -in server/src/main/resources/server.csr -CA root-ca/root-ca.pem -CAkey root-ca/root-ca.key -CAcreateserial -out server/src/main/resources/server-signed.cer -sha256 -extfile server/src/main/resources/extensions/v3.ext -days 1825 -passin pass:secret
```


## Replace un-signed certificate with a signed one
The identity keystore of the server and client still have the unsigned certificate. Now you can replace it with the signed one. It won't be that easy, because the signed certificate does not have the private key so it won't be a valid identity without it. What you need to do is extract the key file from the identity and then merge it back to the identity with the signed certificate. You can do that with the following commands:


### Client

```
keytool -importkeystore -srckeystore client/src/main/resources/identity.jks -destkeystore client/src/main/resources/client.p12 -srcstoretype jks -deststoretype pkcs12 -srcstorepass secret -deststorepass secret
```

```
openssl pkcs12 -in client/src/main/resources/client.p12 -nodes -out client/src/main/resources/client-private.key -nocerts -passin pass:secret
```

```
openssl pkcs12 -export -in client/src/main/resources/client-signed.cer -inkey client/src/main/resources/client-private.key -out client/src/main/resources/client-signed.p12 -name client -passout pass:secret
```

```
keytool -delete -alias client -keystore client/src/main/resources/identity.jks -storepass secret
```

```
keytool -importkeystore -srckeystore client/src/main/resources/client-signed.p12 -srcstoretype PKCS12 -destkeystore client/src/main/resources/identity.jks -srcstorepass secret -deststorepass secret
```

### Server

```
keytool -importkeystore -srckeystore server/src/main/resources/identity.jks -destkeystore server/src/main/resources/server.p12 -srcstoretype jks -deststoretype pkcs12 -srcstorepass secret -deststorepass secret
```

```
openssl pkcs12 -in server/src/main/resources/server.p12 -nodes -out server/src/main/resources/server-private.key -nocerts -passin pass:secret
```

```
openssl pkcs12 -export -in server/src/main/resources/server-signed.cer -inkey server/src/main/resources/server-private.key -out server/src/main/resources/server-signed.p12 -name server -passout pass:secret
```

```
keytool -delete -alias server -keystore server/src/main/resources/identity.jks -storepass secret
```

```
keytool -importkeystore -srckeystore server/src/main/resources/server-signed.p12 -srcstoretype PKCS12 -destkeystore server/src/main/resources/identity.jks -srcstorepass secret -deststorepass secret
```

## Trusting the Certificate Authority only

Now you need to configure your client and server to only trust the Certificate Authority. You can do that by importing the certificate of the Certificate Authority into the truststores of the client and server. You can do that with the following two commands:

### Client

```
keytool -keystore client/src/main/resources/truststore.jks -importcert -file root-ca/root-ca.pem -alias root-ca -storepass secret
```

### Server

```
keytool -keystore server/src/main/resources/truststore.jks -importcert -file root-ca/root-ca.pem -alias root-ca -storepass secret
```

The truststores still contains the client and server specific certificates and that needs to be removed. You can do that with the following command:

Client

```
keytool -keystore client/src/main/resources/truststore.jks -delete -alias server -storepass secret
```

Server

```
keytool -keystore server/src/main/resources/truststore.jks -delete -alias client -storepass secret
```

If you run the client again, you will see that the test passed and that the client received the hello message from the server while based on a certificate which is signed by the Certificate Authority.


## Verify 2-Way TLS 
```
curl -v -XGET --cert client/src/main/resources/client-signed.cer --key client/src/main/resources/client-private.key --cacert root-ca/ca.cer https://localhost:8443/saas-gw-ms/api-1
```