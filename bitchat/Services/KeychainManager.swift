//
// KeychainManager.swift
// bitchat
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

import Foundation
import Security
import os.log

class KeychainManager {
    static let shared = KeychainManager()
    
    // Use consistent service name for all keychain items
    private let service = "chat.bitchat"
    private let appGroup = "group.chat.bitchat"
    
    private init() {
        // Clean up legacy keychain items on first run
        cleanupLegacyKeychainItems()
    }
    
    private func cleanupLegacyKeychainItems() {
        // Check if we've already done cleanup
        let cleanupKey = "bitchat.keychain.cleanup.v2"
        if UserDefaults.standard.bool(forKey: cleanupKey) {
            return
        }
        
        
        // List of old service names to migrate from
        let legacyServices = [
            "com.bitchat.passwords",
            "com.bitchat.deviceidentity",
            "com.bitchat.noise.identity",
            "chat.bitchat.passwords",
            "bitchat.keychain"
        ]
        
        var migratedItems = 0
        
        // Try to migrate identity keys
        for oldService in legacyServices {
            // Check for noise identity key
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: oldService,
                kSecAttrAccount as String: "identity_noiseStaticKey",
                kSecReturnData as String: true
            ]
            
            var result: AnyObject?
            let status = SecItemCopyMatching(query as CFDictionary, &result)
            
            if status == errSecSuccess, let data = result as? Data {
                // Save to new service
                if saveIdentityKey(data, forKey: "noiseStaticKey") {
                    migratedItems += 1
                    SecureLogger.logKeyOperation("migrate", keyType: "noiseStaticKey", success: true)
                }
                // Delete from old service
                let deleteQuery: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: oldService,
                    kSecAttrAccount as String: "identity_noiseStaticKey"
                ]
                SecItemDelete(deleteQuery as CFDictionary)
            }
        }
        
        // Clean up all other legacy items
        for oldService in legacyServices {
            let deleteQuery: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: oldService
            ]
            
            SecItemDelete(deleteQuery as CFDictionary)
        }
        
        
        // Mark cleanup as done
        UserDefaults.standard.set(true, forKey: cleanupKey)
    }
    
    
    private func isSandboxed() -> Bool {
        #if os(macOS)
        let environment = ProcessInfo.processInfo.environment
        return environment["APP_SANDBOX_CONTAINER_ID"] != nil
        #else
        return false
        #endif
    }
    
    // MARK: - Identity Keys
    
    func saveIdentityKey(_ keyData: Data, forKey key: String) -> Bool {
        let fullKey = "identity_\(key)"
        let result = saveData(keyData, forKey: fullKey)
        SecureLogger.logKeyOperation("save", keyType: key, success: result)
        return result
    }
    
    func getIdentityKey(forKey key: String) -> Data? {
        let fullKey = "identity_\(key)"
        return retrieveData(forKey: fullKey)
    }
    
    func deleteIdentityKey(forKey key: String) -> Bool {
        let result = delete(forKey: "identity_\(key)")
        SecureLogger.logKeyOperation("delete", keyType: key, success: result)
        return result
    }
    
    // MARK: - Generic Operations
    
    private func save(_ value: String, forKey key: String) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }
        return saveData(data, forKey: key)
    }
    
    private func saveData(_ data: Data, forKey key: String) -> Bool {
        // Delete any existing item first to ensure clean state
        _ = delete(forKey: key)
        
        // Build query with all necessary attributes for sandboxed apps
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrService as String: service,
            // Important for sandboxed apps: make it accessible when unlocked
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
        ]
        
        // Add a label for easier debugging
        query[kSecAttrLabel as String] = "bitchat-\(key)"
        
        // For sandboxed apps, use the app group for sharing between app instances
        if isSandboxed() {
            query[kSecAttrAccessGroup as String] = appGroup
        }
        
        // For sandboxed macOS apps, we need to ensure the item is NOT synchronized
        #if os(macOS)
        query[kSecAttrSynchronizable as String] = false
        #endif
        
        let status = SecItemAdd(query as CFDictionary, nil)
        
        if status == errSecSuccess {
            return true
        } else if status == -34018 {
            SecureLogger.logError(NSError(domain: "Keychain", code: -34018), context: "Missing keychain entitlement", category: SecureLogger.keychain)
        } else if status != errSecDuplicateItem {
            SecureLogger.logError(NSError(domain: "Keychain", code: Int(status)), context: "Error saving to keychain", category: SecureLogger.keychain)
        }
        
        return false
    }
    
    private func retrieve(forKey key: String) -> String? {
        guard let data = retrieveData(forKey: key) else { return nil }
        return String(data: data, encoding: .utf8)
    }
    
    private func retrieveData(forKey key: String) -> Data? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: service,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        // For sandboxed apps, use the app group
        if isSandboxed() {
            query[kSecAttrAccessGroup as String] = appGroup
        }
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        if status == errSecSuccess {
            return result as? Data
        } else if status == -34018 {
            SecureLogger.logError(NSError(domain: "Keychain", code: -34018), context: "Missing keychain entitlement", category: SecureLogger.keychain)
        }
        
        return nil
    }
    
    private func delete(forKey key: String) -> Bool {
        // Build basic query
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecAttrService as String: service
        ]
        
        // For sandboxed apps, use the app group
        if isSandboxed() {
            query[kSecAttrAccessGroup as String] = appGroup
        }
        
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
    
    // MARK: - Cleanup
    
    func deleteAllPasswords() -> Bool {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword
        ]
        
        // Add service if not empty
        if !service.isEmpty {
            query[kSecAttrService as String] = service
        }
        
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
    
    // Force cleanup to run again (for development/testing)
    func resetCleanupFlag() {
        UserDefaults.standard.removeObject(forKey: "bitchat.keychain.cleanup.v2")
    }
    
    
    // Delete ALL keychain data for panic mode
    func deleteAllKeychainData() -> Bool {
        SecureLogger.log("Panic mode - deleting all keychain data", category: SecureLogger.security, level: .warning)
        
        var totalDeleted = 0
        
        // Search without service restriction to catch all items
        let searchQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecMatchLimit as String: kSecMatchLimitAll,
            kSecReturnAttributes as String: true
        ]
        
        var result: AnyObject?
        let searchStatus = SecItemCopyMatching(searchQuery as CFDictionary, &result)
        
        if searchStatus == errSecSuccess, let items = result as? [[String: Any]] {
            for item in items {
                var shouldDelete = false
                let account = item[kSecAttrAccount as String] as? String ?? ""
                let service = item[kSecAttrService as String] as? String ?? ""
                
                // ONLY delete if service name contains "bitchat"
                // This is the safest approach - we only touch items we know are ours
                if service.lowercased().contains("bitchat") {
                    shouldDelete = true
                }
                
                if shouldDelete {
                    // Build delete query with all available attributes for precise deletion
                    var deleteQuery: [String: Any] = [
                        kSecClass as String: kSecClassGenericPassword
                    ]
                    
                    if !account.isEmpty {
                        deleteQuery[kSecAttrAccount as String] = account
                    }
                    if !service.isEmpty {
                        deleteQuery[kSecAttrService as String] = service
                    }
                    
                    // Add access group if present
                    if let accessGroup = item[kSecAttrAccessGroup as String] as? String,
                       !accessGroup.isEmpty && accessGroup != "test" {
                        deleteQuery[kSecAttrAccessGroup as String] = accessGroup
                    }
                    
                    let deleteStatus = SecItemDelete(deleteQuery as CFDictionary)
                    if deleteStatus == errSecSuccess {
                        totalDeleted += 1
                        SecureLogger.log("Deleted keychain item: \(account) from \(service)", category: SecureLogger.keychain, level: .info)
                    }
                }
            }
        }
        
        // Also try to delete by known service names (in case we missed any)
        let knownServices = [
            "chat.bitchat",
            "com.bitchat.passwords",
            "com.bitchat.deviceidentity", 
            "com.bitchat.noise.identity",
            "chat.bitchat.passwords",
            "bitchat.keychain",
            "bitchat",
            "com.bitchat"
        ]
        
        for serviceName in knownServices {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: serviceName
            ]
            
            let status = SecItemDelete(query as CFDictionary)
            if status == errSecSuccess {
                totalDeleted += 1
            }
        }
        
        SecureLogger.log("Panic mode cleanup completed. Total items deleted: \(totalDeleted)", category: SecureLogger.keychain, level: .warning)
        
        return totalDeleted > 0
    }
    
    // MARK: - Debug
    
    func verifyIdentityKeyExists() -> Bool {
        let key = "identity_noiseStaticKey"
        return retrieveData(forKey: key) != nil
    }
    
    // Aggressive cleanup for legacy items - can be called manually
    func aggressiveCleanupLegacyItems() -> Int {
        var deletedCount = 0
        
        // List of KNOWN bitchat service names from our development history
        let knownBitchatServices = [
            "com.bitchat.passwords",
            "com.bitchat.deviceidentity",
            "com.bitchat.noise.identity",
            "chat.bitchat.passwords",
            "bitchat.keychain",
            "Bitchat",
            "BitChat"
        ]
        
        // First, delete all items from known legacy services
        for legacyService in knownBitchatServices {
            let deleteQuery: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: legacyService
            ]
            
            let status = SecItemDelete(deleteQuery as CFDictionary)
            if status == errSecSuccess {
                deletedCount += 1
            }
        }
        
        // Now search for items that have our specific account patterns with bitchat service names
        let searchQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecMatchLimit as String: kSecMatchLimitAll,
            kSecReturnAttributes as String: true
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(searchQuery as CFDictionary, &result)
        
        if status == errSecSuccess, let items = result as? [[String: Any]] {
            for item in items {
                let account = item[kSecAttrAccount as String] as? String ?? ""
                let service = item[kSecAttrService as String] as? String ?? ""
                
                // ONLY delete if service name contains "bitchat" somewhere
                // This ensures we never touch other apps' keychain items
                var shouldDelete = false
                
                // Check if service contains "bitchat" (case insensitive) but NOT our current service
                let serviceLower = service.lowercased()
                if service != self.service && serviceLower.contains("bitchat") {
                    shouldDelete = true
                }
                
                if shouldDelete {
                    // Build precise delete query
                    let deleteQuery: [String: Any] = [
                        kSecClass as String: kSecClassGenericPassword,
                        kSecAttrService as String: service,
                        kSecAttrAccount as String: account
                    ]
                    
                    let deleteStatus = SecItemDelete(deleteQuery as CFDictionary)
                    if deleteStatus == errSecSuccess {
                        deletedCount += 1
                    }
                }
            }
        }
        
        return deletedCount
    }
}