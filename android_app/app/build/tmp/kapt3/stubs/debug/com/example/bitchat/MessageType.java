package com.example.bitchat;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u0005\n\u0002\b\u0018\b\u0086\u0081\u0002\u0018\u0000 \u001a2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u001aB\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017j\u0002\b\u0018j\u0002\b\u0019\u00a8\u0006\u001b"}, d2 = {"Lcom/example/bitchat/MessageType;", "", "id", "", "(Ljava/lang/String;IB)V", "getId", "()B", "ANNOUNCE", "LEAVE", "MESSAGE", "FRAGMENT_START", "FRAGMENT_CONTINUE", "FRAGMENT_END", "DELIVERY_ACK", "DELIVERY_STATUS_REQUEST", "READ_RECEIPT", "NOISE_HANDSHAKE_INIT", "NOISE_HANDSHAKE_RESP", "NOISE_ENCRYPTED", "NOISE_IDENTITY_ANNOUNCE", "VERSION_HELLO", "VERSION_ACK", "PROTOCOL_ACK", "PROTOCOL_NACK", "SYSTEM_VALIDATION", "HANDSHAKE_REQUEST", "Companion", "app_debug"})
public enum MessageType {
    /*public static final*/ ANNOUNCE /* = new ANNOUNCE(0) */,
    /*public static final*/ LEAVE /* = new LEAVE(0) */,
    /*public static final*/ MESSAGE /* = new MESSAGE(0) */,
    /*public static final*/ FRAGMENT_START /* = new FRAGMENT_START(0) */,
    /*public static final*/ FRAGMENT_CONTINUE /* = new FRAGMENT_CONTINUE(0) */,
    /*public static final*/ FRAGMENT_END /* = new FRAGMENT_END(0) */,
    /*public static final*/ DELIVERY_ACK /* = new DELIVERY_ACK(0) */,
    /*public static final*/ DELIVERY_STATUS_REQUEST /* = new DELIVERY_STATUS_REQUEST(0) */,
    /*public static final*/ READ_RECEIPT /* = new READ_RECEIPT(0) */,
    /*public static final*/ NOISE_HANDSHAKE_INIT /* = new NOISE_HANDSHAKE_INIT(0) */,
    /*public static final*/ NOISE_HANDSHAKE_RESP /* = new NOISE_HANDSHAKE_RESP(0) */,
    /*public static final*/ NOISE_ENCRYPTED /* = new NOISE_ENCRYPTED(0) */,
    /*public static final*/ NOISE_IDENTITY_ANNOUNCE /* = new NOISE_IDENTITY_ANNOUNCE(0) */,
    /*public static final*/ VERSION_HELLO /* = new VERSION_HELLO(0) */,
    /*public static final*/ VERSION_ACK /* = new VERSION_ACK(0) */,
    /*public static final*/ PROTOCOL_ACK /* = new PROTOCOL_ACK(0) */,
    /*public static final*/ PROTOCOL_NACK /* = new PROTOCOL_NACK(0) */,
    /*public static final*/ SYSTEM_VALIDATION /* = new SYSTEM_VALIDATION(0) */,
    /*public static final*/ HANDSHAKE_REQUEST /* = new HANDSHAKE_REQUEST(0) */;
    private final byte id = 0;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bitchat.MessageType.Companion Companion = null;
    
    MessageType(byte id) {
    }
    
    public final byte getId() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.bitchat.MessageType> getEntries() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0005\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/example/bitchat/MessageType$Companion;", "", "()V", "fromId", "Lcom/example/bitchat/MessageType;", "id", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.bitchat.MessageType fromId(byte id) {
            return null;
        }
    }
}