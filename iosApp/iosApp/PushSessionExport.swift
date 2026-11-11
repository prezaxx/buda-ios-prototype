import Foundation
import CryptoKit
import Security

enum PushSessionExport {
    enum ExportError: LocalizedError {
        case invalidKey, invalidSession, encryption
        var errorDescription: String? {
            switch self {
            case .invalidKey: return "Nieprawidłowy klucz połączenia. Pobierz go z prywatnej strony Buda Powiadomienia."
            case .invalidSession: return "Brak poprawnej sesji dziennika. Zaloguj się ponownie w Budzie."
            case .encryption: return "Nie udało się zaszyfrować sesji."
            }
        }
    }
    static func encrypt(session: String, keyFile: Data) throws -> Data {
        guard keyFile.count < 10_000,
              let config = try JSONSerialization.jsonObject(with: keyFile) as? [String: Any],
              config["version"] as? Int == 1,
              let encoded = config["publicKey"] as? String,
              let keyData = Data(base64Encoded: encoded),
              let rootData = session.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: rootData) as? [String: Any],
              let students = root["students"] as? [[String: Any]], !students.isEmpty, students.count <= 10
        else { throw ExportError.invalidSession }
        // Export only the API session. Message cookies and passwords are never included.
        let minimal = students.compactMap { student -> [String: Any]? in
            guard let account = student["account"] as? [String: Any],
                  let credential = student["credential"] as? [String: Any] else { return nil }
            let allowed = ["type", "restUrl", "certificate", "privateKey", "fingerprint", "deviceId", "deviceOs", "deviceModel"]
            return ["account": account, "credential": credential.filter { allowed.contains($0.key) }]
        }
        guard minimal.count == students.count else { throw ExportError.invalidSession }
        let clear = try JSONSerialization.data(withJSONObject: ["version": 1, "students": minimal])
        guard clear.count <= 1_000_000 else { throw ExportError.invalidSession }
        var keyError: Unmanaged<CFError>?
        guard let publicKey = SecKeyCreateWithData(keyData as CFData, [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits as String: 2048
        ] as CFDictionary, &keyError),
        SecKeyIsAlgorithmSupported(publicKey, .encrypt, .rsaEncryptionOAEPSHA256)
        else { throw ExportError.invalidKey }
        let symmetric = SymmetricKey(size: .bits256)
        let raw = symmetric.withUnsafeBytes { Data($0) }
        var error: Unmanaged<CFError>?
        guard let wrapped = SecKeyCreateEncryptedData(publicKey, .rsaEncryptionOAEPSHA256, raw as CFData, &error) as Data?
        else { throw ExportError.encryption }
        let sealed = try AES.GCM.seal(clear, using: symmetric)
        return try JSONSerialization.data(withJSONObject: [
            "version": 1,
            "wrappedKey": wrapped.base64EncodedString(),
            "iv": Data(sealed.nonce).base64EncodedString(),
            "ciphertext": (sealed.ciphertext + sealed.tag).base64EncodedString()
        ], options: [.sortedKeys])
    }
}
