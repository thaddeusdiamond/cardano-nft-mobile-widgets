//
//  ApewatchApp.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/24/22.
//

import os
import SwiftyJSON

struct PortfolioInfo {
    var numAssets : Int = 0
    var numProjects : Int = 0
    var adaFloorEstimate : Double = 0.0
    var adaValueEstimate : Double = 0.0
    var adaToUsd : Double = 0.0
}

struct ApewatchApp {
    
    static let APEWATCHAPP_API : String = "https://apewatch.app/address"
    static let APEWATCHAPP_STDOPTS : String = "?status=owned%2Clisted&show=nfts"
    static let APEWATCH_REQD_HEADERS : [String: String] = [
        "Content-Type": "application/json",
        "x-inertia-partial-component": "address/show",
        "x-inertia": "true",
        "x-inertia-partial-data": "assets",
        "x-inertia-version": "096a8c34ea37cce3f200dba88000a3b0"
    ]
    
    static let TRANSFERRED = "transferred"
    
    static let COINGECKO_ADA_API : String = "https://api.coingecko.com/api/v3/simple/price?ids=cardano&vs_currencies=usd"
    
    static func getPortfolioValue(address: String) -> PortfolioInfo? {
        do {
            let metadata : JSON = try getAsJson(url: "\(ApewatchApp.APEWATCHAPP_API)/\(address)?\(ApewatchApp.APEWATCHAPP_STDOPTS)", headers: ApewatchApp.APEWATCH_REQD_HEADERS)
            os_log("%s", log: PoolPm.LOGGER, type: .debug, metadata.stringValue)
            
            let currPrice : JSON = try getAsJson(url: ApewatchApp.COINGECKO_ADA_API)
            let adaToUsd = currPrice["cardano"]["usd"].doubleValue
            let assets = metadata["props"]["assets"]
            guard let nfts = assets["nfts"].array else {
                return PortfolioInfo(adaFloorEstimate: 0.0, adaValueEstimate: 0.0, adaToUsd: adaToUsd)
            }

            var numAssets : Int = 0
            var numProjects : Int = 0
            var adaFloorEstimate : Double = 0.0
            var adaValueEstimate : Double = 0.0
            for nftCollection : JSON in nfts {
                numProjects += 1
                adaValueEstimate += nftCollection["value"].doubleValue
                for nft : JSON in nftCollection["assets"].arrayValue {
                    if (nft["status"].stringValue == ApewatchApp.TRANSFERRED) {
                        continue
                    }
                    numAssets += 1
                    if nft["quantity"].doubleValue == 1 {
                        adaFloorEstimate += nft["value"].doubleValue
                    }
                }
            }
            
            return PortfolioInfo(
                numAssets: numAssets,
                numProjects: numProjects,
                adaFloorEstimate: adaFloorEstimate,
                adaValueEstimate: adaValueEstimate,
                adaToUsd: adaToUsd
            )
        } catch {
            // Some generic error occurred, better luck next time
        }
        return nil
    }
    
}
