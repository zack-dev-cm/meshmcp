package com.example.bitchat;

/**
 * Manages the ephemeral peer identifier used during this app session.
 * The identifier is an 8 byte value generated from a cryptographically
 * secure random source and stored only in memory. Call [rotate] to
 * refresh the identifier at runtime.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\b\u001a\u00020\u0004H\u0002J\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0005\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u000b"}, d2 = {"Lcom/example/bitchat/PeerIdentityManager;", "", "()V", "currentId", "", "peerId", "getPeerId", "()[B", "generateId", "rotate", "", "app_debug"})
public final class PeerIdentityManager {
    @org.jetbrains.annotations.NotNull()
    private static byte[] currentId;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bitchat.PeerIdentityManager INSTANCE = null;
    
    private PeerIdentityManager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final byte[] getPeerId() {
        return null;
    }
    
    /**
     * Generates and sets a new random peer ID.
     * This can be invoked periodically to reduce linkability
     * between sessions.
     */
    @kotlin.jvm.Synchronized()
    public final synchronized void rotate() {
    }
    
    private final byte[] generateId() {
        return null;
    }
}