package net.nathcat.calendar

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.json.simple.JSONObject;

/**
 * Provide SSLContext from a Let's Encrypt certificate chain
 *
 * <h3>[config] format</h3> <pre> {
 *      "certchain-path": String,
 *      "privatekey-path": String
 * } </pre>
 * @author Nathan Baines
 * @param config The user specified SSL configuration
 */
internal class LetsEncryptProvider(private val config: JSONObject) {
    companion object {
        private fun parseDERFromPEM(
                pem: ByteArray,
                delimStart: String,
                delimEnd: String
        ): ByteArray {
            var d = String(pem)
            var tokens = d.split(delimStart)
            tokens = tokens[1].split(delimEnd)
            return Base64.getDecoder().decode(tokens[0].replace("\n", ""))
        }

        public fun generatePrivateKeyFromDER(key: ByteArray): PrivateKey {
            var spec: PKCS8EncodedKeySpec = PKCS8EncodedKeySpec(key)
            var f: KeyFactory = KeyFactory.getInstance("EC")
            return f.generatePrivate(spec)
        }

        private fun generateCertificateFromDER(cert: ByteArray): X509Certificate {
            var f = CertificateFactory.getInstance("X.509")
            return f.generateCertificate(ByteArrayInputStream(cert)) as X509Certificate
        }

        private fun getBytes(path: String): ByteArray {
            try {
                var fis = FileInputStream(path)
                return fis.readAllBytes()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        private fun getCertificateChain(fullChainPEM: String): Array<Certificate> {
            var pem = String(getBytes(fullChainPEM))

            return Arrays.stream(pem.split("-----BEGIN CERTIFICATE-----").toTypedArray())
                .filter { return@filter !it.equals("") }
                .map {
                    try {
                        return@map generateCertificateFromDER(
                            Base64.getDecoder().decode(
                                (it as String).split("-----END CERTIFICATE-----")[0].replace("\n", "")
                            )
                        )
                    } catch (e: CertificateException) {
                        throw RuntimeException(e)
                    }
                }.toList().toTypedArray()
        }
    }

    public fun getContext(): SSLContext? {
        try {
            var context = SSLContext.getInstance("TLS")
            var keyBytes = parseDERFromPEM(getBytes(config.get("privatekey-path") as String), "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----")

            var certs = getCertificateChain(config.get("certchain-path") as String)
            var key = generatePrivateKeyFromDER(keyBytes)

            var keyStore = KeyStore.getInstance("JKS")
            keyStore.load(null)
            keyStore.setCertificateEntry("cert-alias", certs[0])
            keyStore.setKeyEntry("key-alias", key, "pass".toCharArray(), certs)

            var kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, "pass".toCharArray())

            context.init(kmf.getKeyManagers(), null, null)
            return context
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
