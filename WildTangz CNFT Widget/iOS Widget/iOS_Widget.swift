//
//  iOS_Widget.swift
//  iOS Widget
//
//  Created by Thaddeus Diamond on 7/12/22.
//

import os
import WidgetKit
import SwiftUI
import Intents
import SwiftyJSON

struct RandomNftEntry: TimelineEntry {
        
    let date: Date
    let configuration: ConfigurationIntent
    let nftInfo: NftInfo?
    
}

struct RandomNftProvider: IntentTimelineProvider {
    
    static let LOGGER = OSLog(subsystem: AppConstants.CONFIG_GROUP_NAME, category: "main")
    
    static let MAX_LOAD_ATTEMPTS = 5
    static let BACKOFF : UInt32 = 2
    
    func placeholder(in context: Context) -> RandomNftEntry {
        return RandomNftEntry(date: Date(), configuration: ConfigurationIntent(), nftInfo: NftInfo())
    }

    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (RandomNftEntry) -> ()) {
        if context.isPreview {
            let entry = RandomNftEntry(date: Date(), configuration: configuration, nftInfo: NftInfo())
            completion(entry)   
        }
    }

    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        guard let address = UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) else {
            return
        }
        
        let currentDate : Date = Date()
        for _ in 1...RandomNftProvider.MAX_LOAD_ATTEMPTS {
            do {
                let nftInfo : NftInfo? = PoolPm.getNftFromAddrString(addressOrAsset: address)
                let entries: [RandomNftEntry] = [
                    RandomNftEntry(date: currentDate, configuration: configuration, nftInfo: nftInfo)
                ]

                let reloadDate = Calendar.current.date(byAdding: .minute, value: 15, to: currentDate)!
                let timeline = Timeline(entries: entries, policy: .after(reloadDate))
                return completion(timeline)
            } catch {
                sleep(RandomNftProvider.BACKOFF)
            }
        } 
    }
    
}

struct iOS_WidgetEntryView : View {

    static let PLACEHOLDER_IMG_NAME = "WidgetImage"

    var entry: RandomNftEntry

    var body: some View {
        autoreleasepool {
            ZStack {
                if entry.nftInfo == nil || entry.nftInfo?.mediaType == nil {
                    Color("WidgetImageBG")
                    Image(uiImage: UIImage(named: iOS_WidgetEntryView.PLACEHOLDER_IMG_NAME)!).resizable().scaledToFit()
                } else {
                    Color(.black)
                    switch entry.nftInfo?.mediaType {
                    case NftInfo.SVG_IMAGE_TYPE:
                        entry.nftInfo?.asSVGNode()?.toSwiftUI()
                    default:
                        Image(uiImage: (entry.nftInfo?.asUIImage())!).resizable().scaledToFit()
                    }
                }
            }
        }
    }
    
}

@main
struct iOS_Widget: Widget {
    let kind: String = AppConstants.CONFIG_GROUP_NAME

    var body: some WidgetConfiguration {
        IntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: RandomNftProvider()) { entry in
            iOS_WidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Cardano NFT Widget")
        .description("Display a random Cardano NFT from your wallet.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge])
    }
}
