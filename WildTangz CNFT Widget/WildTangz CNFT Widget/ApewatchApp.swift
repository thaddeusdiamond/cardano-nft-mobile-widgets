//
//  ApewatchApp.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/24/22.
//

import Foundation
import os
import SwiftyJSON

struct PortfolioInfo {
    var numAssets : Int = 0
    var numProjects : Int = 0
    var adaValueEstimate : Double = 0.0
    var fiatEstimate : Double = 0.0
    var fiatCurrencyStr : String = ""
}

struct ApewatchApp {
    
    static let ADA_KEY = "ADA"
    static let USD_KEY = "USD"
    static let USD_SYMBOL = "$"

    static let APEWATCH_API = "https://apewatch.app/api/v1/account"

    static let ASSETS_KEY = "assets"
    static let COUNTS_KEY = "counts"
    static let DEFAULT_KEY = "default"
    static let POLICIES_KEY = "policies"
    static let TOTAL_KEY = "total"
    static let VALUES_KEY = "values"
    
    static func getPortfolioValue(address: String) -> PortfolioInfo? {
        do {
            let portfolioInfo : JSON = try getAsJson(url: "\(ApewatchApp.APEWATCH_API)/\(address)")
            os_log("%s", log: PoolPm.LOGGER, type: .debug, portfolioInfo.stringValue)
            
            let nfts = portfolioInfo[VALUES_KEY][TOTAL_KEY][DEFAULT_KEY]

            let nftValuations = nfts[VALUES_KEY]
            let adaValueEstimate = nftValuations[ADA_KEY].doubleValue
            let fiatEstimate : Double
            let fiatCurrencyStr : String
            if nftValuations[Locale.current.currencyCode!].exists() {
                fiatEstimate = nftValuations[Locale.current.currencyCode!].doubleValue
                fiatCurrencyStr = Locale.current.currencySymbol!
            } else {
                fiatEstimate = nftValuations[USD_KEY].doubleValue
                fiatCurrencyStr = USD_SYMBOL
            }

            let counts = portfolioInfo[COUNTS_KEY]
            let numProjects = counts[POLICIES_KEY].intValue
            let numAssets = counts[ASSETS_KEY][TOTAL_KEY].intValue
            
            return PortfolioInfo(
                numAssets: numAssets,
                numProjects: numProjects,
                adaValueEstimate: adaValueEstimate,
                fiatEstimate: fiatEstimate,
                fiatCurrencyStr: fiatCurrencyStr
            )
        } catch {
            // Some generic error occurred, better luck next time
        }
        return nil
    }
    
}
