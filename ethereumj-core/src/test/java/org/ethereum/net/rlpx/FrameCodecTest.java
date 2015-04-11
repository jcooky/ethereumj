package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by devrandom on 2015-04-11.
 */
public class FrameCodecTest {
    private FrameCodec iCodec;
    private FrameCodec rCodec;

    @Before
    public void setUp() throws IOException {
        ECKey remoteKey = new ECKey().decompress();
        ECKey myKey = new ECKey().decompress();
        EncryptionHandshake initiator = new EncryptionHandshake(remoteKey.getPubKeyPoint());
        EncryptionHandshake responder = new EncryptionHandshake();
        AuthInitiateMessage initiate = initiator.createAuthInitiate(null, myKey);
        AuthResponseMessage response = responder.handleAuthInitiate(initiate, remoteKey);
        initiator.handleAuthResponse(initiate, response);
        PipedInputStream to = new PipedInputStream(1024*1024);
        PipedOutputStream toOut = new PipedOutputStream(to);
        PipedInputStream from = new PipedInputStream(1024*1024);
        PipedOutputStream fromOut = new PipedOutputStream(from);
        iCodec = new FrameCodec(initiator.getSecrets(), to, fromOut);
        rCodec = new FrameCodec(responder.getSecrets(), from, toOut);
    }

    @Test
    public void testFrame() throws Exception {
        byte[] payload = new byte[123];
        new SecureRandom().nextBytes(payload);
        FrameCodec.Frame frame = new FrameCodec.Frame(12345, 123, new ByteArrayInputStream(payload));
        iCodec.writeFrame(frame);
        FrameCodec.Frame frame1 = rCodec.readFrame();
        byte[] payload1 = new byte[frame1.size];
        assertEquals(frame.size, frame1.size);
        frame1.payload.read(payload1);
        assertArrayEquals(payload, payload1);
        assertEquals(frame.type, frame1.type);
    }

}