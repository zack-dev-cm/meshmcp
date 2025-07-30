package com.example.bitchat;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u0005\n\u0002\b\t\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000b\u00a8\u0006\f"}, d2 = {"Lcom/example/bitchat/NoiseMessageType;", "", "id", "", "(Ljava/lang/String;IB)V", "getId", "()B", "HANDSHAKE_INITIATION", "HANDSHAKE_RESPONSE", "HANDSHAKE_FINAL", "ENCRYPTED_MESSAGE", "SESSION_RENEGOTIATION", "app_debug"})
public enum NoiseMessageType {
    /*public static final*/ HANDSHAKE_INITIATION /* = new HANDSHAKE_INITIATION(0) */,
    /*public static final*/ HANDSHAKE_RESPONSE /* = new HANDSHAKE_RESPONSE(0) */,
    /*public static final*/ HANDSHAKE_FINAL /* = new HANDSHAKE_FINAL(0) */,
    /*public static final*/ ENCRYPTED_MESSAGE /* = new ENCRYPTED_MESSAGE(0) */,
    /*public static final*/ SESSION_RENEGOTIATION /* = new SESSION_RENEGOTIATION(0) */;
    private final byte id = 0;
    
    NoiseMessageType(byte id) {
    }
    
    public final byte getId() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.bitchat.NoiseMessageType> getEntries() {
        return null;
    }
}