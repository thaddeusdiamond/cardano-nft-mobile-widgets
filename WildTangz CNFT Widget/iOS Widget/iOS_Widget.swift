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

    static let PLACEHOLDER_IMG_NAME = "WidgetImage"

    var entry: RandomNftEntry

    var body: some View {
        autoreleasepool {
            ZStack {
                let imageData : Data? = entry.imageData
                if imageData != nil, let uiImage : UIImage = UIImage(data: imageData!) {
                    Color(.black)
                    Image(uiImage: uiImage).resizable().scaledToFit()
                } else if imageData != nil, let xml = DOMParser.parse(data: imageData!) {
                    Color(.black)
                    SVGView(xml: xml)
                } else {
                    let defaultImage = UIImage(named: iOS_WidgetEntryView.PLACEHOLDER_IMG_NAME)!
                    Image(uiImage: defaultImage).resizable().scaledToFit()
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
