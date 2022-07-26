//
//  Blockfrost.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/25/22.
//

import Foundation
import SwiftyJSON

class Blockfrost {
    
    static let HANDLE_IDENTIFIER = "$"
    static let HANDLE_POLICY_ID = "f0ff48bbb7bbe9d59a40f1ce90e9e9d0ff5002ec48f232b49ca0fb9a"
    
    private static let BLOCKFROST_URL = "https://cardano-mainnet.blockfrost.io/api/v0"
    private static let BLOCKFROST_CARDANO_KEY : String = Bundle.main.infoDictionary?["BLOCKFROST_CARDANO_KEY"] as! String
    private static let BLOCKFROST_IPFS_KEY : String = Bundle.main.infoDictionary?["BLOCKFROST_IPFS_KEY"] as! String
    private static let BLOCKFROST_IPFS_GATEWAY = "https://ipfs.blockfrost.io/api/v0/ipfs/gateway"
    
    static func getAssetInfo(_ assetHexName : String) -> JSON {
        return callBlockfrostApi("assets/\(assetHexName)")
    }
    
    static func getNfts(addressOrHandle: String) -> [JSON] {
        var assetsArray : JSON
        if addressOrHandle.starts(with: Blockfrost.HANDLE_IDENTIFIER) {
            let accountAddress = lookupHandleAddr(handle: addressOrHandle)
            assetsArray = callBlockfrostApi("accounts/\(accountAddress)/addresses/assets")
        } else {
            let addressInfo = callBlockfrostApi("addresses/\(addressOrHandle)")
            if addressInfo["stake_address"].null != nil {
                assetsArray = addressInfo["amount"]
            } else {
                assetsArray = callBlockfrostApi("accounts/\(addressInfo["stake_address"].stringValue)/addresses/assets")
            }
        }
        return assetsArray.arrayValue.filter { $0["quantity"].intValue == 1 }
    }
    
    private static func lookupHandleAddr(handle: String) -> String {
        let rawHandle = handle[handle.index(handle.startIndex, offsetBy: Blockfrost.HANDLE_IDENTIFIER.count)...]
        let assetName = Data(rawHandle.lowercased().utf8).map { String(format:"%02x", $0) }.joined()
        let addressesForHandle = callBlockfrostApi("assets/\(Blockfrost.HANDLE_POLICY_ID)\(assetName)/addresses")
        let addressForHandle = addressesForHandle[0]["address"].stringValue
        let addressInfo = callBlockfrostApi("addresses/\(addressForHandle)")
        return addressInfo["stake_address"].stringValue
    }
    
    private static func callBlockfrostApi(_ endpoint: String) -> JSON {
        do {
            return try getAsJson(
                url: "\(Blockfrost.BLOCKFROST_URL)/\(endpoint.lowercased())",
                headers: ["project_id": Blockfrost.BLOCKFROST_CARDANO_KEY, "Content-Type": "application/json"]
            )
        } catch {
            return JSON()
        }
    }
    
    static func getDataFromIpfs(_ ipfsId: String) -> Data? {
        return try? getAsData(
            url: URL(string: "\(Blockfrost.BLOCKFROST_IPFS_GATEWAY)/\(ipfsId)")!,
            headers: ["project_id": Blockfrost.BLOCKFROST_IPFS_KEY, "Content-Type": "application/json"]
        )!
    }
    
}
