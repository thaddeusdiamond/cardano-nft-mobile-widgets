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
    
    static func getNftFromAddrString(addressOrAsset: String) -> NftInfo? {
        if addressOrAsset.starts(with: PoolPm.ASSET_PREFIX) {
            return getAssetData(assetId: addressOrAsset)
        }
        return getRandomNft(address: addressOrAsset)
    }
    
    static private func getAssetData(assetId: String) -> NftInfo? {
        do {
            let token : JSON = try getAsJson(url: "\(PoolPm.POOLPM_ASSET_API)/\(assetId)")
            if isNft(token: token) {
                return getNftImage(token: token)
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
    
    static private func getNftImage(token: JSON) -> NftInfo? {
        let imageJson = token["metadata"]["image"]
        let mediaType = token["metadata"]["mediaType"].stringValue
        if let imageUrl = imageJson.string {
            let webUrl = convertedToWeb(imageUrl: imageUrl)
            os_log("%s", log: PoolPm.LOGGER, type: .info, webUrl)
            return NftInfo(mediaType: mediaType, imageUrl: URL(string: webUrl))
        } else if let imageDataArr = imageJson.array {
            let imageDataConcat : String = imageDataArr.map({ (subData : JSON) in subData.stringValue }).reduce("", +)
            let imageDataInlineStart : String.Index = imageDataConcat.firstIndex(of: ",")!
            let imageDataInline = String(imageDataConcat[imageDataConcat.index(imageDataInlineStart, offsetBy: 1)...])
            if imageDataConcat.range(of: "base64,") != nil {
                return NftInfo(mediaType: mediaType, imageData: Data(base64Encoded: imageDataInline))
            }
            return NftInfo(mediaType: mediaType, imageData: Data(imageDataInline.utf8))
        }
        return nil
    }
    
    static private func getRandomNft(address: String) -> NftInfo? {
        do {
            let metadata : JSON = try getAsJson(url: "\(PoolPm.POOLPM_WALLET_API)/\(address)")
            os_log("%s", log: PoolPm.LOGGER, type: .debug, metadata.stringValue)
            
            var tokens : [JSON] = []
            for token in metadata["tokens"].arrayValue {
                if isNft(token: token) {
                    tokens.append(token)
                }
            }
            
            if let randomNft = tokens.randomElement() {
                os_log("%s", log: PoolPm.LOGGER, type: .debug, randomNft.rawString()!)
                if let nftImage = getNftImage(token: randomNft) {
                    return nftImage
                }
            }
        } catch {
            // Some generic error occurred, return the default NFT
        }
        return nil
    }
    
    static private func convertedToWeb(imageUrl: String) -> String {
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
