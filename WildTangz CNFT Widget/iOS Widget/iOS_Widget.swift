//
//  iOS_Widget.swift
//  iOS Widget
//
//  Created by Thaddeus Diamond on 7/12/22.
//

import Intents
import SwiftUI
import WidgetKit

@main
struct iOS_Widget: WidgetBundle {

    var body: some Widget {
        RandomNftWidget()
        ApewatchWidget()
    }
    
    struct RandomNftWidget: Widget {
        let kind: String = AppConstants.RANDOM_WIDGET_NAME

        var body: some WidgetConfiguration {
            IntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: RandomNftProvider()) { entry in
                RandomNftWidgetView(entry: entry)
            }
            .configurationDisplayName("Cardano NFT Viewer")
            .description("Display a random Cardano NFT from your wallet")
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge])
        }
    }
        
    struct ApewatchWidget: Widget {
        let kind: String = AppConstants.APEWATCH_WIDGET_NAME

        var body: some WidgetConfiguration {
           IntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: ApewatchProvider()) { entry in
               ApewatchWidgetView(entry: entry)
            }
            .configurationDisplayName("Cardano NFT Portfolio")
            .description("Display portfolio value data (Powered by TapTools)")
            .supportedFamilies([.systemSmall, .systemMedium])
        }
    }
}
