/********************************************************************************

 * Copyright (c) 2023-24 Harman International 

 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.

 * You may obtain a copy of the License at

 *
 *  <p>http://www.apache.org/licenses/LICENSE-2.0

 *     
 * <p>Unless required by applicable law or agreed to in writing, software

 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 * See the License for the specific language governing permissions and

 * limitations under the License.

 *
 * <p>SPDX-License-Identifier: Apache-2.0

 ********************************************************************************/

package org.eclipse.ecsp.hivemq.auth.authentication;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import lombok.Getter;
import lombok.Setter;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

/**
 * This class is used to create a SSL certificate, This is being used in CertificateAuthenticationTest.
 */
public class TestSslClientCertificateImpl implements ClientTlsInformation {

    private String commonName;
    @Getter
    @Setter
    private int days;
    
    private static final Long DAY_IN_MS = 86400000L;
    private static final int SIXTY_FOUR = 64;
    
    /**
     * Returns the common name associated with this SSL client certificate.
     *
     * @return the common name
     */
    public String commonName() {
        return this.commonName;
    }

    /**
     * Sets the common name for the SSL client certificate.
     *
     * @param commonName the common name to set
     */
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    /**
     * Generates an X509 certificate with the specified parameters.
     *
     * @param dn the distinguished name for the certificate
     * @param pair the key pair used to generate the certificate
     * @param days the number of days the certificate is valid for
     * @param algorithm the algorithm used for signing the certificate
     * @return the generated X509 certificate
     * @throws GeneralSecurityException if a security exception occurs during certificate generation
     * @throws IOException if an I/O exception occurs during certificate generation
     */
    private X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException {
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * DAY_IN_MS);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(SIXTY_FOUR, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.MD5_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        PrivateKey privkey = pair.getPrivate();
        cert.sign(privkey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);
        return cert;
    }

    /**
     * This method generates a new X509Certificate certificate.
     *
     * @param days - certificate validity, number of days
     * @param cn - common name
     * @return X509Certificate certificate
     * @throws Exception - exception
     */
    public X509Certificate getCertificate(int days, String cn) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        // String distinguishedName = "CN=" + (StringUtils.isEmpty(cn) ? "Test" : cn) +
        // ", L=London, C=GB";
        String distinguishedName = "CN=" + commonName() + ", L=London, C=GB";
        X509Certificate certificate = generateCertificate(distinguishedName, keyPair, days, "SHA256withRSA");
        return certificate;
    }

    /**
    * Retrieves the client certificate, if available.
    *
    * @return An optional containing the client certificate, or an empty optional if the certificate is 
    *       not available.
    */
    @Override
    public @NotNull Optional<X509Certificate> getClientCertificate() {
        try {
            return Optional.of(getCertificate(days, ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public @NotNull Optional<X509Certificate[]> getClientCertificateChain() {
        return null;
    }

    @Override
    public @NotNull String getCipherSuite() {
        return null;
    }

    @Override
    public @NotNull String getProtocol() {
        return null;
    }

    @Override
    public @NotNull Optional<String> getHostname() {
        return null;
    }
}