package com.example.bitchat;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u001e\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\b\u0018\u0000 02\u00020\u0001:\u00010BS\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u000b\u0012\u0006\u0010\r\u001a\u00020\u000b\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\u0002\u0010\u000fJ\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0005H\u00c6\u0003J\t\u0010!\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\"\u001a\u00020\tH\u00c6\u0003J\t\u0010#\u001a\u00020\u000bH\u00c6\u0003J\u000b\u0010$\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\t\u0010%\u001a\u00020\u000bH\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\b\u0010\'\u001a\u00020\u0007H\u0002J]\u0010(\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u000b2\b\b\u0002\u0010\r\u001a\u00020\u000b2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000bH\u00c6\u0001J\u0013\u0010)\u001a\u00020*2\b\u0010+\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010,\u001a\u00020\u0003H\u00d6\u0001J\u0006\u0010-\u001a\u00020\u000bJ\t\u0010.\u001a\u00020/H\u00d6\u0001R\u0011\u0010\r\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0013\u0010\f\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0011R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0011R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0011R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u001a\u0010\u0006\u001a\u00020\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001aR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001e\u00a8\u00061"}, d2 = {"Lcom/example/bitchat/BitchatPacket;", "", "version", "", "type", "Lcom/example/bitchat/MessageType;", "ttl", "", "timestamp", "", "senderId", "", "recipientId", "payload", "signature", "(ILcom/example/bitchat/MessageType;BJ[B[B[B[B)V", "getPayload", "()[B", "getRecipientId", "getSenderId", "getSignature", "getTimestamp", "()J", "getTtl", "()B", "setTtl", "(B)V", "getType", "()Lcom/example/bitchat/MessageType;", "getVersion", "()I", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "computeFlags", "copy", "equals", "", "other", "hashCode", "toBytes", "toString", "", "Companion", "app_debug"})
public final class BitchatPacket {
    private final int version = 0;
    @org.jetbrains.annotations.NotNull()
    private final com.example.bitchat.MessageType type = null;
    private byte ttl;
    private final long timestamp = 0L;
    @org.jetbrains.annotations.NotNull()
    private final byte[] senderId = null;
    @org.jetbrains.annotations.Nullable()
    private final byte[] recipientId = null;
    @org.jetbrains.annotations.NotNull()
    private final byte[] payload = null;
    @org.jetbrains.annotations.Nullable()
    private final byte[] signature = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bitchat.BitchatPacket.Companion Companion = null;
    
    public BitchatPacket(int version, @org.jetbrains.annotations.NotNull()
    com.example.bitchat.MessageType type, byte ttl, long timestamp, @org.jetbrains.annotations.NotNull()
    byte[] senderId, @org.jetbrains.annotations.Nullable()
    byte[] recipientId, @org.jetbrains.annotations.NotNull()
    byte[] payload, @org.jetbrains.annotations.Nullable()
    byte[] signature) {
        super();
    }
    
    public final int getVersion() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bitchat.MessageType getType() {
        return null;
    }
    
    public final byte getTtl() {
        return 0;
    }
    
    public final void setTtl(byte p0) {
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] getSenderId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final byte[] getRecipientId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] getPayload() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final byte[] getSignature() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] toBytes() {
        return null;
    }
    
    private final byte computeFlags() {
        return 0;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bitchat.MessageType component2() {
        return null;
    }
    
    public final byte component3() {
        return 0;
    }
    
    public final long component4() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final byte[] component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final byte[] component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.bitchat.BitchatPacket copy(int version, @org.jetbrains.annotations.NotNull()
    com.example.bitchat.MessageType type, byte ttl, long timestamp, @org.jetbrains.annotations.NotNull()
    byte[] senderId, @org.jetbrains.annotations.Nullable()
    byte[] recipientId, @org.jetbrains.annotations.NotNull()
    byte[] payload, @org.jetbrains.annotations.Nullable()
    byte[] signature) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001:\u0001\u0007B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\b"}, d2 = {"Lcom/example/bitchat/BitchatPacket$Companion;", "", "()V", "from", "Lcom/example/bitchat/BitchatPacket;", "data", "", "Flags", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.example.bitchat.BitchatPacket from(@org.jetbrains.annotations.NotNull()
        byte[] data) {
            return null;
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0005\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/bitchat/BitchatPacket$Companion$Flags;", "", "()V", "HAS_RECIPIENT", "", "HAS_SIGNATURE", "IS_COMPRESSED", "app_debug"})
        public static final class Flags {
            public static final byte HAS_RECIPIENT = (byte)1;
            public static final byte HAS_SIGNATURE = (byte)2;
            public static final byte IS_COMPRESSED = (byte)4;
            @org.jetbrains.annotations.NotNull()
            public static final com.example.bitchat.BitchatPacket.Companion.Flags INSTANCE = null;
            
            private Flags() {
                super();
            }
        }
    }
}