package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECIESCoder;
import org.ethereum.crypto.ECKey;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by android on 4/8/15.
 */
public class HandshakeTest {
    private ECKey myKey;
    private ECKey remoteKey;
    private Handshake initiator;

    @Before
    public void setUp() {
        remoteKey = new ECKey().decompress();
        myKey = new ECKey().decompress();
        initiator = new Handshake(remoteKey.getPubKeyPoint());
    }

    @Test
    public void testCreateAuthInitiate() throws Exception {
        AuthInitiateMessage message = initiator.createAuthInitiate(new byte[32], myKey);
        int expectedLength = 65+32+64+32+1;
        byte[] buffer = message.encode();
        assertEquals(expectedLength, buffer.length);
    }

    @Test
    public void testAgreement() throws Exception {
        Handshake responder = new Handshake();
        AuthInitiateMessage initiate = initiator.createAuthInitiate(null, myKey);
        AuthResponseMessage response = responder.handleAuthInitiate(initiate);
        initiator.handleAuthResponse(response);
        assertArrayEquals(initiator.getSecrets().aes, responder.getSecrets().aes);
        assertArrayEquals(initiator.getSecrets().mac, responder.getSecrets().mac);
        assertArrayEquals(initiator.getSecrets().token, responder.getSecrets().token);
    }

    @Test
    public void testAgreementWithEncryption() throws Exception {
        Handshake responder = new Handshake();
        AuthInitiateMessage initiate = initiator.createAuthInitiate(null, myKey);
        byte[] authBuffer = initiator.encryptAuthMessage(initiate);

        // Simulate decryption
        byte[] plaintext = ECIESCoder.decrypt(remoteKey.getPrivKey(), authBuffer);
        assertArrayEquals(plaintext, initiate.encode());

        AuthResponseMessage response = responder.handleAuthInitiate(initiate);
        // Simulate encryption
        byte[] responseBuffer = ECIESCoder.encrypt(responder.getRemotePublicKey(), response.encode());

        // Decryption should preserve
        AuthResponseMessage response1 = initiator.decryptAuthResponse(responseBuffer, myKey);
        assertArrayEquals(response.encode(), response1.encode());
        initiator.handleAuthResponse(response);
        assertArrayEquals(initiator.getSecrets().aes, responder.getSecrets().aes);
        assertArrayEquals(initiator.getSecrets().mac, responder.getSecrets().mac);
        assertArrayEquals(initiator.getSecrets().token, responder.getSecrets().token);
    }
}