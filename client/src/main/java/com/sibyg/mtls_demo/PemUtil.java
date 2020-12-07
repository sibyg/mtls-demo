package com.sibyg.mtls_demo;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class PemUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    private static PemObject pemObject(String filePath) throws IOException {
        try (PemReader pemReader = new PemReader(new FileReader(new File(filePath)))) {
            return pemReader.readPemObject();
        }
    }

    public static PrivateKey readPrivateKey(String filePath) throws Exception {
        return KeyFactory.getInstance("RSA", "BC")
                .generatePrivate(new PKCS8EncodedKeySpec(pemObject(filePath).getContent()));
    }


    public static X509Certificate getx509Certificate(File crtFile) throws Exception {
        // Get the private key
        FileReader reader = new FileReader(crtFile);
        PEMReader pem = new PEMReader(reader);

        X509Certificate x509Certificate = (X509Certificate) pem.readObject();

        pem.close();

        reader.close();

        return x509Certificate;
    }


}
