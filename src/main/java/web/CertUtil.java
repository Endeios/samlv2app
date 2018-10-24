package web;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertUtil {
    static X509Certificate getCertificateByName(String certificateAlias, String trustStoreName, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            loadKeyStore(keyStore, trustStoreName, password);
            Certificate cert = keyStore.getCertificate(certificateAlias);
            if (cert.getType().equals("X.509")) {
                return (X509Certificate) cert;
            }
            throw new Error("Could not find a suitable x509 certificate for alias " + certificateAlias + " in " + trustStoreName);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new Error("Error opening keystore: " + e.getCause(), e);
        }
    }

    private static void loadKeyStore(KeyStore keyStore, String jksPath, String jksPassword) throws IOException, NoSuchAlgorithmException,
            CertificateException {
        char[] password = null;
        if (jksPassword != null) {
            password = jksPassword.toCharArray();
        }
        keyStore.load(ClassLoader.getSystemResourceAsStream(jksPath), password);
    }
}
