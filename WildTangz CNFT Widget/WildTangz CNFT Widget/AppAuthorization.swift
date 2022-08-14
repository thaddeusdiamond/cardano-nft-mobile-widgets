//
//  AppAuthorization.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/27/22.
//

class AppAuthorization {
    
    private static let REQUIRED_POLICY : String = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"
    private static let REQUIRED_NAME : String = "Wild Tangz"
    private static let REQUIRED_FOR_VIEWER : Int = 1
    private static let REQUIRED_FOR_PORTFOLIO : Int = 3
    
    static func isAuthorizedForViewer(addressOrAsset: String) -> Bool {
        PoolPm.numRequiredAssets(addressOrAsset: addressOrAsset, policy: REQUIRED_POLICY) >= REQUIRED_FOR_VIEWER
    }
    
    static func unauthorizedForViewerMsg() -> String {
        "Address needs at least \(AppAuthorization.REQUIRED_FOR_VIEWER) \(AppAuthorization.REQUIRED_NAME) NFT(s)"
    }
    
    static func isAuthorizedForPortfolio(addressOrAsset: String) -> Bool {
        PoolPm.numRequiredAssets(addressOrAsset: addressOrAsset, policy: REQUIRED_POLICY) >= REQUIRED_FOR_PORTFOLIO
    }
    
    static func unauthorizedForPortfolioMsg() -> String {
        "Wallet does not have \(AppAuthorization.REQUIRED_FOR_PORTFOLIO) \(AppAuthorization.REQUIRED_NAME)"
    }
    
}
