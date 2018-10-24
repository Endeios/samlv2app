package web;

import java.security.cert.X509Certificate;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class LoadKeyTest {
    @Test
    public void testCanGetCertificateFromTrustStore() {
        X509Certificate certificate = CertUtil.getCertificateByName("test", "truststore.jks", null);
        Assert.assertNotNull(certificate, "certificate should never be null");
    }

}
