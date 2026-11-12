import Foundation
import Security
import CryptoKit

@main struct PushExportTests {
 static func main() throws {
  var error: Unmanaged<CFError>?
  let privateKey = SecKeyCreateRandomKey([kSecAttrKeyType as String:kSecAttrKeyTypeRSA,kSecAttrKeySizeInBits as String:2048] as CFDictionary,&error)!
  let publicKey = SecKeyCopyPublicKey(privateKey)!
  let publicData = SecKeyCopyExternalRepresentation(publicKey,&error)! as Data
  let keyFile = try JSONSerialization.data(withJSONObject:["version":1,"publicKey":publicData.base64EncodedString()])
  let session = #"{"version":1,"students":[{"account":{"Pupil":{"Id":1}},"credential":{"privateKey":"synthetic","password":"must-not-export"},"cookies":[{"value":"must-not-export"}]}]}"#
  let exported = try PushSessionExport.encrypt(session:session,keyFile:keyFile)
  let envelope = try JSONSerialization.jsonObject(with:exported) as! [String:Any]
  let wrapped = Data(base64Encoded:envelope["wrappedKey"] as! String)!
  let raw = SecKeyCreateDecryptedData(privateKey,.rsaEncryptionOAEPSHA256,wrapped as CFData,&error)! as Data
  let nonce = Data(base64Encoded:envelope["iv"] as! String)!
  let ciphertext = Data(base64Encoded:envelope["ciphertext"] as! String)!
  let box = try AES.GCM.SealedBox(nonce:AES.GCM.Nonce(data:nonce),ciphertext:ciphertext.dropLast(16),tag:ciphertext.suffix(16))
  let decoded = try AES.GCM.open(box,using:SymmetricKey(data:raw))
  let text = String(data:decoded,encoding:.utf8)!
  precondition(!text.contains("must-not-export") && !text.contains("cookies") && !text.contains("password"))
  precondition(text.contains("synthetic"))
  do { _ = try PushSessionExport.encrypt(session:"KEYCHAIN_UNAVAILABLE",keyFile:keyFile);fatalError("Invalid session accepted") } catch {}
  for invalid in [Data("not json".utf8), Data(#"{"version":2,"publicKey":"invalid"}"#.utf8)] {
   do { _ = try PushSessionExport.encrypt(session:session,keyFile:invalid);fatalError("Invalid key accepted") }
   catch PushSessionExport.ExportError.invalidKey {}
   catch { fatalError("Wrong invalid-key error") }
  }
  do { _ = try PushSessionExport.encrypt(session:"KEYCHAIN_UNAVAILABLE",keyFile:keyFile);fatalError("Invalid session accepted") }
  catch PushSessionExport.ExportError.invalidSession {}
  catch { fatalError("Wrong missing-session error") }
  print("Push export: RSA-OAEP/AES-GCM round trip and session minimization passed.")
 }
}
