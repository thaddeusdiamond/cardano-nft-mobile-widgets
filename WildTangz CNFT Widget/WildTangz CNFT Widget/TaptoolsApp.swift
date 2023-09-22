//
//  TaptoolsApp.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 9/12/23.
//

import Foundation
import os
import SwiftyJSON

struct TaptoolsApp {
    
    private static let LOGGER = OSLog(subsystem: "group.wildtangz", category: "taptools")

    private static let TAPTOOLS_API = "https://openapi.taptools.io/api/v1"
    private static let TAPTOOLS_API_KEY : String = Bundle.main.infoDictionary?["TAPTOOLS_API_KEY"] as! String
    
    private static let COINMARKETCAP_API = "https://pro-api.coinmarketcap.com"
    private static let COINMARKETCAP_API_KEY : String = Bundle.main.infoDictionary?["COINMARKETCAP_API_KEY"] as! String
    
    static let ADA_KEY = "ADA"
    static let USD_KEY = "USD"
    static let USD_SYMBOL = "$"
    
    static let NUM_NFTS_KEY = "numNFTs"
    static let ADA_VALUE_KEY = "adaValue"
    static let POSITIONS_NFTS_KEY = "positionsNft"
    
    static func getPortfolioValue(address: String) -> PortfolioInfo? {
        do {
            var accountAddress = address;
            if address.starts(with: Blockfrost.HANDLE_IDENTIFIER) {
                accountAddress = Blockfrost.lookupHandleAddr(handle: address)
            }
            
            let portfolioInfo : JSON = try getAsJson(
                url: "\(TAPTOOLS_API)/wallet/portfolio/positions?address=\(accountAddress)",
                headers: ["x-api-key" : TAPTOOLS_API_KEY]
            )
            os_log("%s", log: LOGGER, type: .debug, portfolioInfo.stringValue)
            
            let currencyCode = Locale.current.currencyCode!
            let conversionInfo : JSON = try getAsJson(
                url: "\(COINMARKETCAP_API)/v2/cryptocurrency/quotes/latest?symbol=\(ADA_KEY)&convert=USD,\(currencyCode)",
                headers: ["X-CMC_PRO_API_KEY": COINMARKETCAP_API_KEY]
            )
            os_log("%s", log: LOGGER, type: .debug, conversionInfo.stringValue)

            let numProjects = portfolioInfo[NUM_NFTS_KEY].intValue
            var numAssets = 0;
            for nftPosition : JSON in portfolioInfo[POSITIONS_NFTS_KEY].arrayValue {
                numAssets += nftPosition["balance"].intValue
            }
            
            let adaConversionRate : Double
            let fiatCurrencyStr : String
            let quotes : JSON = conversionInfo["data"][ADA_KEY].array![0]["quote"]
            if quotes[currencyCode].exists() {
                adaConversionRate = quotes[currencyCode]["price"].doubleValue
                fiatCurrencyStr = Locale.current.currencySymbol!
            } else {
                adaConversionRate = quotes[USD_KEY]["price"].doubleValue
                fiatCurrencyStr = USD_SYMBOL
            }

            let adaValueEstimate = portfolioInfo[ADA_VALUE_KEY].doubleValue
            let fiatEstimate : Double = adaValueEstimate * adaConversionRate;
            
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
