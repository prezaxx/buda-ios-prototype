import Foundation
import Security
import ComposeApp

final class KeychainVault: NSObject, SessionVault {
    private let account: String
    init(account: String = "diary-session-v1") { self.account = account; super.init() }
    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: (Bundle.main.bundleIdentifier ?? "pl.buda.ios.prototype") + ".session",
         kSecAttrAccount as String: account]
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
          kSecAttrAccessible as String: UserDefaults.standard.bool(forKey: "gradeChecksEnabled") ? kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly : kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        let status = SecItemUpdate(query as CFDictionary, attrs as CFDictionary)
        if status == errSecSuccess { return true }
        guard status == errSecItemNotFound else { return false }
        var q = query
        attrs.forEach { q[$0.key] = $0.value }
        return SecItemAdd(q as CFDictionary, nil) == errSecSuccess
    }
    func setBackgroundAccess(_ enabled: Bool) -> Bool {
        let access = enabled ? kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly : kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let result = SecItemUpdate(query as CFDictionary, [kSecAttrAccessible as String: access] as CFDictionary)
        return result == errSecSuccess || result == errSecItemNotFound
    }
    func clear() -> Bool {
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
