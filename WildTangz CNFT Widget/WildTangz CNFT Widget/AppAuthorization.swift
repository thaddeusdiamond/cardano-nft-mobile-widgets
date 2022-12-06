//
//  AppAuthorization.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/27/22.
//
import SwiftyJSON

class AppAuthorization {
    
    //private static let REQUIRED_POLICY : String = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"
    private static let REQUIRED_NAME : String = "Wild Tangz"
    private static let REQUIRED_FOR_VIEWER : Int = 1
    private static let REQUIRED_FOR_PORTFOLIO : Int = 3
    
    private static let TOKEN_GATE_NFT_VIEWER : [String: Int] = [
        "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b": 1,
        "33566617519280305e147975f80914cea1c93e8049567829f7370fca": 1,
        "335695f7771bb789083b8a985308310ab8f0a4bbf8cd0687bbdb26b1": 1
    ]
    
    private static let TOKEN_GATE_NFT_PORTFOLIO : [String: Int] = [
        "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b": 3,
        "33566617519280305e147975f80914cea1c93e8049567829f7370fca": 1
    ]
    
    static func isAuthorized(assetsMap: [String: Int], tokenGateMap: [String: Int]) -> Bool {
        let assetsAuthorized = assetsMap.map { (key, val) -> Bool in
            val >= tokenGateMap[key]!
        }
        return assetsAuthorized.reduce(false) { acc, auth -> Bool in acc || auth  }
    }
    
    static func isAuthorizedForViewer(addressOrAsset: String) -> Bool {
        let assetsMap = PoolPm.numRequiredAssets(addressOrAsset: addressOrAsset, policies: Array(TOKEN_GATE_NFT_VIEWER.keys))
        return isAuthorized(assetsMap: assetsMap, tokenGateMap: TOKEN_GATE_NFT_VIEWER)
    }
    
    static func unauthorizedForViewerMsg() -> String {
        "Address needs at least \(AppAuthorization.REQUIRED_FOR_VIEWER) \(AppAuthorization.REQUIRED_NAME) NFT(s)"
    }
    
    static func isAuthorizedForPortfolio(addressOrAsset: String) -> Bool {
        let assetsMap = PoolPm.numRequiredAssets(addressOrAsset: addressOrAsset, policies: Array(TOKEN_GATE_NFT_PORTFOLIO.keys))
        return isAuthorized(assetsMap: assetsMap, tokenGateMap: TOKEN_GATE_NFT_PORTFOLIO)
    }
    
    static func unauthorizedForPortfolioMsg() -> String {
        "Wallet does not have \(AppAuthorization.REQUIRED_FOR_PORTFOLIO) \(AppAuthorization.REQUIRED_NAME)"
    }
    
}
