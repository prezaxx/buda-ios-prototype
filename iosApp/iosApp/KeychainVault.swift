import Foundation
import Security
import ComposeApp

final class KeychainVault: NSObject, SessionVault {
    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: (Bundle.main.bundleIdentifier ?? "pl.buda.ios.prototype") + ".session",
         kSecAttrAccount as String: "diary-session-v1"]
    }
    func read() -> String? {
        var q = query
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var value: CFTypeRef?
        let status = SecItemCopyMatching(q as CFDictionary, &value)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = value as? Data,
              let text = String(data: data, encoding: .utf8) else { return "KEYCHAIN_UNAVAILABLE" }
        return text
    }
    func write(value: String) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }
        let attrs: [String: Any] = [kSecValueData as String: data,
          kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        let status = SecItemUpdate(query as CFDictionary, attrs as CFDictionary)
        if status == errSecSuccess { return true }
        guard status == errSecItemNotFound else { return false }
        var q = query
        attrs.forEach { q[$0.key] = $0.value }
        return SecItemAdd(q as CFDictionary, nil) == errSecSuccess
    }
    func clear() -> Bool {
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
