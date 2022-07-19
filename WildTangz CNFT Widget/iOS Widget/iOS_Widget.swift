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
import SVGView

struct RandomNftEntry: TimelineEntry {
    
    static let DEFAULT_BG_COLOR : UInt = 0x7E7D81
    static let PLACEHOLDER_IMG_NAME = "DefaultWidgetImage"
    
    let date: Date
    let configuration: ConfigurationIntent
    let imageData: Data?
}

struct RandomNftProvider: IntentTimelineProvider {
    
    static let LOGGER = OSLog(subsystem: AppConstants.CONFIG_GROUP_NAME, category: "main")
    
    func placeholder(in context: Context) -> RandomNftEntry {
        return RandomNftEntry(date: Date(), configuration: ConfigurationIntent(), imageData: nil)
    }

    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (RandomNftEntry) -> ()) {
        if context.isPreview {
            let entry = RandomNftEntry(date: Date(), configuration: configuration, imageData: nil)
            completion(entry)   
        }
    }

    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        guard let address = UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) else {
            return
        }
        
        let currentDate : Date = Date()
        let imageData : Data? = PoolPm.getNftFromAddrString(addressOrAsset: address)
        let entries: [RandomNftEntry] = [
            RandomNftEntry(date: currentDate, configuration: configuration, imageData: imageData)
        ]

        let reloadDate = Calendar.current.date(byAdding: .minute, value: 15, to: currentDate)!
        let timeline = Timeline(entries: entries, policy: .after(reloadDate))
        completion(timeline)
    }
    
}

struct iOS_WidgetEntryView : View {
    var entry: RandomNftEntry

    var body: some View {
        autoreleasepool {
            ZStack {
                if let imageData : Data = entry.imageData {
                    Color(.black)
                    if let uiImage : UIImage = UIImage(data: imageData) {
                        Image(uiImage: uiImage).resizable().scaledToFit()
                    } else if let xml = DOMParser.parse(data: imageData) {
                        SVGView(xml: xml)
                    }
                } else {
                    Color(RandomNftEntry.DEFAULT_BG_COLOR)
                    Image(uiImage: UIImage(named: RandomNftEntry.PLACEHOLDER_IMG_NAME)!).resizable().scaledToFit()
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
