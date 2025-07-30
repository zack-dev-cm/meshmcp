package com.example.bitchat;

/**
 * Basic Noise XX encryption service.
 * Generates Curve25519 keys and performs the XX handshake over BLE.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001:\u0002\"#B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0013H\u0002J\u0010\u0010\u0014\u001a\u00020\u00062\u0006\u0010\u0002\u001a\u00020\u0003H\u0002J\u0016\u0010\u0015\u001a\u00020\f2\u0006\u0010\u0016\u001a\u00020\t2\u0006\u0010\u0017\u001a\u00020\fJ\u0016\u0010\u0018\u001a\u00020\f2\u0006\u0010\u0016\u001a\u00020\t2\u0006\u0010\u0019\u001a\u00020\fJ\u000e\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u0016\u001a\u00020\tJ\u0018\u0010\u001c\u001a\u0004\u0018\u00010\u001b2\u0006\u0010\u0016\u001a\u00020\t2\u0006\u0010\u001d\u001a\u00020\u001bJ\u000e\u0010\u001e\u001a\u00020\u001f2\u0006\u0010\u0016\u001a\u00020\tJ\u0006\u0010 \u001a\u00020!R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\n0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\r\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006$"}, d2 = {"Lcom/example/bitchat/NoiseEncryptionService;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "prefs", "Landroidx/security/crypto/EncryptedSharedPreferences;", "sessions", "Ljava/util/concurrent/ConcurrentHashMap;", "", "Lcom/example/bitchat/NoiseEncryptionService$Session;", "staticPrivate", "", "staticPublic", "getStaticPublic", "()[B", "createHandshake", "Lcom/southernstorm/noise/protocol/HandshakeState;", "role", "", "createPrefs", "decrypt", "peerId", "ciphertext", "encrypt", "plaintext", "initiateHandshake", "Lcom/example/bitchat/NoiseMessage;", "receiveHandshakeMessage", "message", "status", "Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;", "wipeAll", "", "EncryptionStatus", "Session", "app_debug"})
public final class NoiseEncryptionService {
    @org.jetbrains.annotations.NotNull()
    private final androidx.security.crypto.EncryptedSharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final byte[] staticPrivate = null;
    @org.jetbrains.annotations.NotNull()
    private final byte[] staticPublic = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, com.example.bitchat.NoiseEncryptionService.Session> sessions = null;
    
    public NoiseEncryptionService(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] getStaticPublic() {
        return null;
    }
    
    private final androidx.security.crypto.EncryptedSharedPreferences createPrefs(android.content.Context context) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bitchat.NoiseMessage initiateHandshake(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.bitchat.NoiseMessage receiveHandshakeMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    com.example.bitchat.NoiseMessage message) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] encrypt(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    byte[] plaintext) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] decrypt(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    byte[] ciphertext) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bitchat.NoiseEncryptionService.EncryptionStatus status(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
        return null;
    }
    
    public final void wipeAll() {
    }
    
    private final com.southernstorm.noise.protocol.HandshakeState createHandshake(int role) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;", "", "(Ljava/lang/String;I)V", "NONE", "NO_HANDSHAKE", "NOISE_HANDSHAKING", "NOISE_SECURED", "app_debug"})
    public static enum EncryptionStatus {
        /*public static final*/ NONE /* = new NONE() */,
        /*public static final*/ NO_HANDSHAKE /* = new NO_HANDSHAKE() */,
        /*public static final*/ NOISE_HANDSHAKING /* = new NOISE_HANDSHAKING() */,
        /*public static final*/ NOISE_SECURED /* = new NOISE_SECURED() */;
        
        EncryptionStatus() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.example.bitchat.NoiseEncryptionService.EncryptionStatus> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0082\b\u0018\u00002\u00020\u0001B7\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\u000b\u0010\u001d\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\nH\u00c6\u0003J?\u0010\u001f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\t\u001a\u00020\nH\u00c6\u0001J\u0013\u0010 \u001a\u00020!2\b\u0010\"\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010#\u001a\u00020$H\u00d6\u0001J\t\u0010%\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u001c\u0010\b\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u001c\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u000f\"\u0004\b\u0013\u0010\u0011R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u001a\u0010\t\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0019\u00a8\u0006&"}, d2 = {"Lcom/example/bitchat/NoiseEncryptionService$Session;", "", "sessionId", "", "handshake", "Lcom/southernstorm/noise/protocol/HandshakeState;", "sendCipher", "Lcom/southernstorm/noise/protocol/CipherState;", "recvCipher", "status", "Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;", "(Ljava/lang/String;Lcom/southernstorm/noise/protocol/HandshakeState;Lcom/southernstorm/noise/protocol/CipherState;Lcom/southernstorm/noise/protocol/CipherState;Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;)V", "getHandshake", "()Lcom/southernstorm/noise/protocol/HandshakeState;", "getRecvCipher", "()Lcom/southernstorm/noise/protocol/CipherState;", "setRecvCipher", "(Lcom/southernstorm/noise/protocol/CipherState;)V", "getSendCipher", "setSendCipher", "getSessionId", "()Ljava/lang/String;", "getStatus", "()Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;", "setStatus", "(Lcom/example/bitchat/NoiseEncryptionService$EncryptionStatus;)V", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    static final class Session {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String sessionId = null;
        @org.jetbrains.annotations.NotNull()
        private final com.southernstorm.noise.protocol.HandshakeState handshake = null;
        @org.jetbrains.annotations.Nullable()
        private com.southernstorm.noise.protocol.CipherState sendCipher;
        @org.jetbrains.annotations.Nullable()
        private com.southernstorm.noise.protocol.CipherState recvCipher;
        @org.jetbrains.annotations.NotNull()
        private com.example.bitchat.NoiseEncryptionService.EncryptionStatus status;
        
        public Session(@org.jetbrains.annotations.NotNull()
        java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
        com.southernstorm.noise.protocol.HandshakeState handshake, @org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState sendCipher, @org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState recvCipher, @org.jetbrains.annotations.NotNull()
        com.example.bitchat.NoiseEncryptionService.EncryptionStatus status) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getSessionId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.southernstorm.noise.protocol.HandshakeState getHandshake() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.southernstorm.noise.protocol.CipherState getSendCipher() {
            return null;
        }
        
        public final void setSendCipher(@org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.southernstorm.noise.protocol.CipherState getRecvCipher() {
            return null;
        }
        
        public final void setRecvCipher(@org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.bitchat.NoiseEncryptionService.EncryptionStatus getStatus() {
            return null;
        }
        
        public final void setStatus(@org.jetbrains.annotations.NotNull()
        com.example.bitchat.NoiseEncryptionService.EncryptionStatus p0) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.southernstorm.noise.protocol.HandshakeState component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.southernstorm.noise.protocol.CipherState component3() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.southernstorm.noise.protocol.CipherState component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.bitchat.NoiseEncryptionService.EncryptionStatus component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.bitchat.NoiseEncryptionService.Session copy(@org.jetbrains.annotations.NotNull()
        java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
        com.southernstorm.noise.protocol.HandshakeState handshake, @org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState sendCipher, @org.jetbrains.annotations.Nullable()
        com.southernstorm.noise.protocol.CipherState recvCipher, @org.jetbrains.annotations.NotNull()
        com.example.bitchat.NoiseEncryptionService.EncryptionStatus status) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}