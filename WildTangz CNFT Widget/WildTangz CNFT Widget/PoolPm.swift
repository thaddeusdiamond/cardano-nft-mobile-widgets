//
//  PoolPm.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/13/22.
//

import os
import Foundation
import SwiftyJSON

class PoolPm {
    
    static let LOGGER = OSLog(subsystem: "group.wildtangz", category: "poolpm")
    
    static let POOLPM_BASE = "https://pool.pm"
    static let POOLPM_ASSET_API = "\(POOLPM_BASE)/asset"

    static let ASSET_PREFIX : String = "asset"

    static let IPFS_PROTOCOL : String = "ipfs://"
    static let IPFS_V1_START : String.Element = "Q"
    static let IPFS_V2_START : String = "baf"
    
    static let MAX_ATTEMPTS : Int = 3
    
    static func getNftFromAddrString(addressOrAsset: String) -> NftInfo? {
        if addressOrAsset.starts(with: PoolPm.ASSET_PREFIX) {
            return getAssetData(assetId: addressOrAsset)
        }
        return getRandomNft(address: addressOrAsset)
    }
    
    static private func getAssetData(assetId: String) -> NftInfo? {
        do {
            let token : JSON = try getAsJson(url: PoolPm.getAssetUrlFor(assetId))
            return getNftImage(tokenMetadata: token["metadata"])
        } catch {
            // Some generic error occurred, return the default NFT
        }
        return nil
    }
    
    static private func getNftImage(tokenMetadata: JSON) -> NftInfo? {
        let imageJson = tokenMetadata["image"]
        let mediaType = tokenMetadata["mediaType"].stringValue
        if let imageUrl = imageJson.string {
            let webUrl = ipfsAwareString(imageUrl: imageUrl)
            os_log("%s", log: PoolPm.LOGGER, type: .info, webUrl!)
            return NftInfo(mediaType: mediaType, imageUrl: webUrl)
        } else if let imageDataArr = imageJson.array {
            let imageDataConcat : String = imageDataArr.map({ (subData : JSON) in subData.stringValue }).reduce("", +)
            if imageDataConcat.starts(with: NftInfo.DATA_PREFIX) {
                let imageDataInlineStart : String.Index = imageDataConcat.firstIndex(of: ",")!
                let imageDataInline = String(imageDataConcat[imageDataConcat.index(imageDataInlineStart, offsetBy: 1)...])
                if imageDataConcat.range(of: "base64,") != nil {
                    return NftInfo(mediaType: mediaType, imageData: Data(base64Encoded: imageDataInline))
                }
                return NftInfo(mediaType: mediaType, imageData: Data(imageDataInline.utf8))
            } else {
                os_log("%s", log: PoolPm.LOGGER, type: .info, imageDataConcat)
                return NftInfo(mediaType: mediaType, imageUrl: ipfsAwareString(imageUrl: imageDataConcat))
            }
        }
        return nil
    }
    
    static private func getRandomNft(address: String) -> NftInfo? {
        let tokens = Blockfrost.getNfts(addressOrHandle: address)
        for _ in 1...PoolPm.MAX_ATTEMPTS {
            guard let randomToken = tokens.randomElement() else {
                continue
            }
            os_log("%s", log: PoolPm.LOGGER, type: .debug, randomToken.stringValue)
            let token : JSON = Blockfrost.getAssetInfo(randomToken["unit"].stringValue)
            if let nftImage = getNftImage(tokenMetadata: token["onchain_metadata"]) {
                if nftImage.svgNode == nil, nftImage.uiImage == nil {
                    continue
                }
                return nftImage
            }
        }
        return nil
    }
    
    static private func ipfsAwareString(imageUrl: String) -> String? {
        if imageUrl.starts(with: PoolPm.IPFS_PROTOCOL) {
            if let cidStart = imageUrl.firstIndex(of: PoolPm.IPFS_V1_START) {
                return String(imageUrl[cidStart..<imageUrl.endIndex])
            } else if let cidStart = imageUrl.range(of: PoolPm.IPFS_V2_START)?.lowerBound {
                return String(imageUrl[cidStart..<imageUrl.endIndex])
            }
            return nil
        }
        
        return imageUrl
    }
    
    static func numRequiredAssets(addressOrAsset: String, policies: [String]) -> [String: Int] {
        do {
            if addressOrAsset.starts(with: PoolPm.ASSET_PREFIX) {
                let poolInfo : JSON = try getAsJson(url: PoolPm.getAssetUrlFor(addressOrAsset))
                return numRequiredAssets(addressOrAsset: poolInfo["owner"].stringValue, policies: policies)
            }
            let nfts : [JSON] = Blockfrost.getNfts(addressOrHandle: addressOrAsset)
            var policyCounts : [String: Int] = [:]
            for nft in nfts {
                //let policyId = String(nft["unit"].stringValue.prefix(56))
                let policyId = policies.first(where: { policy -> Bool in nft["unit"].stringValue.starts(with: policy) })
                if policyId != nil {
                    let currentCount = policyCounts[policyId!]
                    policyCounts[policyId!] = (currentCount == nil) ? 1 : (currentCount! + 1)
                }
            }
            return policyCounts
        } catch {
            return [:]
        }
    }
    
    static private func getAssetUrlFor(_ selection : String) -> String {
        return "\(PoolPm.POOLPM_ASSET_API)/\(selection.lowercased())"
    }
    
}
