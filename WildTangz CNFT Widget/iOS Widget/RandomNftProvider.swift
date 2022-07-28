//
//  RandomNftProvider.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/24/22.
//

import Intents
import os
import SwiftUI
import SwiftyJSON
import WidgetKit

struct RandomNftEntry: TimelineEntry {
        
    let date: Date
    let configuration: ConfigurationIntent
    
    let selection: String
    let isAuthorized: Bool
    let nftInfo: NftInfo?
    
}

struct RandomNftProvider: IntentTimelineProvider {
    
    static let LOGGER = OSLog(subsystem: AppConstants.CONFIG_GROUP_NAME, category: "main")
    
    static let MAX_LOAD_ATTEMPTS = 5
    static let BACKOFF : UInt32 = 2
    
    static let REFRESH_AUTH_MINS = 15
    static let REFRESH_UNAUTH_MINS = 120
    
    func placeholder(in context: Context) -> RandomNftEntry {
        return RandomNftEntry(date: Date(), configuration: ConfigurationIntent(), selection: AppConstants.SAMPLE_HANDLE, isAuthorized: true, nftInfo: NftInfo())
    }

    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (RandomNftEntry) -> ()) {
        if context.isPreview {
            let entry = RandomNftEntry(date: Date(), configuration: configuration, selection: AppConstants.SAMPLE_HANDLE, isAuthorized: true, nftInfo: NftInfo())
            completion(entry)
        }
    }

    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<RandomNftEntry>) -> ()) {
        guard let address = UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) else {
            return
        }
        
        let currentDate : Date = Date()
        let isAuthorized = AppAuthorization.isAuthorizedForViewer(addressOrAsset: address)
        let reloadDate = reloadDateFor(currentDate: currentDate, isAuthorized: isAuthorized)
        for _ in 1...RandomNftProvider.MAX_LOAD_ATTEMPTS {
            do {
                var nftInfo : NftInfo? = nil
                if isAuthorized {
                    nftInfo = PoolPm.getNftFromAddrString(addressOrAsset: address)
                }
                let entries: [RandomNftEntry] = [
                    RandomNftEntry(date: currentDate, configuration: configuration, selection: address, isAuthorized: isAuthorized, nftInfo: nftInfo)
                ]

                let timeline = Timeline(entries: entries, policy: .after(reloadDate))
                completion(timeline)
                return
            } catch {
                sleep(RandomNftProvider.BACKOFF)
            }
        }
    }
    
    private func reloadDateFor(currentDate : Date, isAuthorized : Bool) -> Date {
        if isAuthorized {
            return Calendar.current.date(byAdding: .minute, value: RandomNftProvider.REFRESH_AUTH_MINS, to: currentDate)!
        } else {
            return Calendar.current.date(byAdding: .minute, value: RandomNftProvider.REFRESH_UNAUTH_MINS, to: currentDate)!
        }
    }
    
}

struct RandomNftWidgetView : View {

    static let PLACEHOLDER_IMG_NAME = "WidgetImage"

    var entry: RandomNftEntry

    var body: some View {
        autoreleasepool {
            ZStack {
                if !entry.isAuthorized {
                    VStack {
                        Text(entry.selection).font(.title).fontWeight(.bold)
                        Text(AppAuthorization.unauthorizedForViewerMsg()).font(.title3)
                    }.padding()
                } else if entry.nftInfo == nil || entry.nftInfo?.mediaType == nil {
                    Color("WidgetImageBG")
                    Image(uiImage: UIImage(named: RandomNftWidgetView.PLACEHOLDER_IMG_NAME)!).resizable().scaledToFit()
                } else {
                    Color(.black)
                    switch entry.nftInfo?.mediaType {
                    case NftInfo.SVG_IMAGE_TYPE:
                        entry.nftInfo?.svgNode!.toSwiftUI()
                    default:
                        Image(uiImage: (entry.nftInfo?.uiImage)!).resizable().scaledToFit()
                    }
                }
            }
        }
    }
    
}
