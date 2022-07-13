//
//  PoolPm.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/13/22.
//

import os
import Foundation
import SwiftyJSON

struct PoolPm {
    
    static let LOGGER = OSLog(subsystem: "group.wildtangz", category: "poolpm")
    
    static let POOLPM_BASE = "https://pool.pm"
    static let POOLPM_WALLET_API = "\(POOLPM_BASE)/wallet"
    static let POOLPM_ASSET_API = "\(POOLPM_BASE)/asset"

    static let ASSET_PREFIX : String = "asset"

    static let IPFS_GATEWAY : String = "https://infura-ipfs.io/ipfs"
    static let IPFS_PROTOCOL : String = "ipfs://"
    static let IPFS_V1_START : String.Element = "Q"
    static let IPFS_V2 : String = "bafy"
    
    static func getNftFromAddrString(addressOrAsset: String) -> Data? {
        if addressOrAsset.starts(with: PoolPm.ASSET_PREFIX) {
            return getAssetData(assetId: addressOrAsset)
        }
        return getRandomNft(address: addressOrAsset)
    }
    
    static private func getAssetData(assetId: String) -> Data? {
        do {
            let metadata : JSON = try getAsJson(url: "\(PoolPm.POOLPM_ASSET_API)/\(assetId)")
            if isNft(token: metadata), let imageUrl: String = getNftImageUrl(token: metadata) {
                let webUrl = convertedToWeb(imageUrl: imageUrl)
                os_log("%s", log: PoolPm.LOGGER, type: .info, webUrl)
                return try Data(contentsOf: URL(string: webUrl)!)
            }
        } catch {
            // Some generic error occurred, return the default NFT
        }
        return nil
    }
    
    static private func getAsJson(url: String) throws -> JSON {
        let apiEndpoint : URL = URL(string: url)!
        let metadataData : Data = try Data(contentsOf: apiEndpoint)
        return try JSON(data: metadataData)
    }
    
    static private func isNft(token: JSON) -> Bool {
        return token["quantity"].intValue == 1
    }
    
    static private func getNftImageUrl(token: JSON) -> String? {
        return token["metadata"]["image"].stringValue
    }
    
    static private func getRandomNft(address: String) -> Data? {
        do {
            let metadata : JSON = try getAsJson(url: "\(PoolPm.POOLPM_WALLET_API)/\(address)")
            os_log("%s", log: PoolPm.LOGGER, type: .debug, metadata.stringValue)
            
            var tokens : [String] = []
            for token in metadata["tokens"].arrayValue {
                if isNft(token: token), let imageUrl: String = getNftImageUrl(token: token) {
                    tokens.append(convertedToWeb(imageUrl:   imageUrl))
                }
            }
            
            if let randomNftUrl = tokens.randomElement(), let url = URL(string: randomNftUrl) {
                os_log("%s", log: PoolPm.LOGGER, type: .debug, randomNftUrl)
                return try Data(contentsOf: url)
            }
        } catch {
            // Some generic error occurred, return the default NFT
        }
        return nil
    }
    
    static private func convertedToWeb(imageUrl: String) -> String {
        // TODO: Support on-chain and ghostwatch
        if imageUrl.starts(with: PoolPm.IPFS_PROTOCOL), let cidStart = imageUrl.firstIndex(of: PoolPm.IPFS_V1_START) {
            let cid = imageUrl[cidStart..<imageUrl.endIndex]
            return "\(PoolPm.IPFS_GATEWAY)/\(cid)"
        }
        
        if imageUrl.starts(with: PoolPm.IPFS_V2) {
            return "\(PoolPm.IPFS_GATEWAY)/\(imageUrl)"
        }
        
        return imageUrl
    }
    
    static func getTokenPolicies(addressOrAsset: String) -> [String] {
        do {
            if addressOrAsset.starts(with: PoolPm.ASSET_PREFIX) {
                let poolInfo : JSON = try getAsJson(url: "\(PoolPm.POOLPM_ASSET_API)/\(addressOrAsset)")
                return getTokenPolicies(addressOrAsset: poolInfo["owner"].stringValue)
            }

            let poolInfo : JSON = try getAsJson(url: "\(PoolPm.POOLPM_WALLET_API)/\(addressOrAsset)")
            var policies : [String] = []
            for token in poolInfo["tokens"].arrayValue {
                policies.append(token["policy"].stringValue)
            }
            return policies
        } catch {
            return []
        }
    }
    
}
