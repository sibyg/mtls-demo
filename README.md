1. Create A Self Signed Client (assume F5) Cert
    `keytool -genkeypair -alias f5 -keyalg RSA -keysize 2048 -storetype JKS -keystore f5.jks -validity 3650 -ext SAN=dns:localhost,ip:127.0.0.1` 
    
2.  Create Self Signed Server  (assume SAASGW) Cert:
    `keytool -genkeypair -alias saas-gw-ms -keyalg RSA -keysize 2048 -storetype JKS -keystore  saas-gw-ms.jks -validity 3650 -ext SAN=dns:localhost,ip:127.0.0.1`

3. Create public certificate file from client (assume f5) cert:
    `keytool -exportcert -keystore certificates/f5.jks -storepass secret -alias f5 -rfc -file certificates/f5.cer`

4. Create Public Certificate File From Server (assume SAASGW) Cert:
    `keytool -exportcert -keystore certificates/saas-gw-ms.jks -storepass secret -alias saas-gw-ms -rfc -file certificates/saas-gw-ms.cer`

5. Import Client Cert to Server jks File:
    `keytool -keystore server/src/main/resources/truststore.jks -importcert -file certificates/f5.cer -alias f5 -storepass secret`
6.  Import Server Cert to Client jks File:
  `keytool -keystore client/src/main/resources/truststore.jks -importcert -file certificates/saas-gw-ms.cer -alias saas-gw-ms -storepass secret`
7.  Start the Server:
 `cd client/ && mvn spring-boot:run`
8.  Check from console:
 `curl -i -XGET https://localhost:8443/saas-gw-ms/api-1 --cacert ../certificates/saas-gw-ms.cer`
9. Run the Client:
 `cd client/ && mvn spring-boot:run`