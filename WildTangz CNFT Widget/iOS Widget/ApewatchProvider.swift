//
//  ApewatchProvider.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/24/22.
//

import Intents
import WidgetKit
import SwiftUI
import SwiftyJSON

struct ApewatchEntry: TimelineEntry {
        
    let date: Date
    let configuration: ConfigurationIntent
    
    var selection: String = "$electedHandleGoesHere"
    var numRequiredAssets: Int = AppAuthorization.REQUIRED_FOR_PORTFOLIO
    var portfolioInfo: PortfolioInfo? = nil
    
}

struct ApewatchProvider: IntentTimelineProvider {
    
    static let HOURS_REFRESH: Int = 1
        
    func placeholder(in context: Context) -> ApewatchEntry {
        return ApewatchEntry(date: Date(), configuration: ConfigurationIntent())
    }

    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (ApewatchEntry) -> ()) {
        if context.isPreview {
            let entry = ApewatchEntry(date: Date(), configuration: configuration)
            completion(entry)
        }
    }

    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<ApewatchEntry>) -> ()) {
        guard let address = UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) else {
            return
        }
        
        let currentDate : Date = Date()
        let portfolioInfo : PortfolioInfo? = ApewatchApp.getPortfolioValue(address: address)
        let numRequiredAssets : Int = PoolPm.numRequiredAssets(address: address, policy: AppAuthorization.REQUIRED_POLICY)
        let entries: [ApewatchEntry] = [
            ApewatchEntry(
                date: currentDate,
                configuration: configuration,
                selection: address,
                numRequiredAssets: numRequiredAssets,
                portfolioInfo: portfolioInfo
            )
        ]

        let reloadDate = Calendar.current.date(byAdding: .hour, value: ApewatchProvider.HOURS_REFRESH, to: currentDate)!
        let timeline = Timeline(entries: entries, policy: .after(reloadDate))
        return completion(timeline)
    }
    
}

struct ApewatchWidgetView : View {
    
    static let LINE_LIMIT : Int = 1
    static let ADA_SYMBOL : String = "₳"
    static let USD_SYMBOL : String = "$"
    
    static let ADA_VALUE_DEFAULT : Double = 1234.56
    static let ADA_FLOOR_DEFAULT : Double = 567.89
    static let ADA_TO_USD_DEFAULT : Double = 0.5
    
    static let NUM_ASSETS_DEFAULT : Int = 666
    static let NUM_PROJECTS_DEFAULT : Int = 33
    
    static let WILDTANGZ_LOGO_NAME = "WidgetImageSmall"
    static let WILDTANGZ_LOGO_HEIGHT = 40.0
    
    static private func getFormattedString(value: Double, currency: String) -> String {
        let formatter : NumberFormatter = currencyFormatterFor(currencySymbol: currency)
        if let formattedValue : String = formatter.string(from: value as NSNumber) {
            return formattedValue
        }
        return "(n/a)"
    }
    
    
    static private func currencyFormatterFor(currencySymbol: String) -> NumberFormatter {
        let formatter = NumberFormatter()
        formatter.currencySymbol = currencySymbol
        formatter.numberStyle = .currency
        return formatter
    }
    
    @Environment(\.widgetFamily) var family
    
    var entry: ApewatchEntry

    var body: some View {
        VStack(alignment: .leading) {
            let numAssets = entry.portfolioInfo?.numAssets ?? ApewatchWidgetView.NUM_ASSETS_DEFAULT
            let numProjects = entry.portfolioInfo?.numProjects ?? ApewatchWidgetView.NUM_PROJECTS_DEFAULT
            let valueAda = entry.portfolioInfo?.adaValueEstimate ?? ApewatchWidgetView.ADA_VALUE_DEFAULT
            let floorAda = entry.portfolioInfo?.adaFloorEstimate ?? ApewatchWidgetView.ADA_FLOOR_DEFAULT
            let adaToUsd = entry.portfolioInfo?.adaToUsd ?? ApewatchWidgetView.ADA_TO_USD_DEFAULT
            let valueUsd = valueAda * adaToUsd
            //let floorUsd = floorAda * adaToUsd
            
            LazyVGrid(columns: [
                GridItem(.flexible(), alignment: .center),
                GridItem(.fixed(ApewatchWidgetView.WILDTANGZ_LOGO_HEIGHT), alignment: .center)
            ]) {
                Text("**\(entry.selection)**").font(.title2).lineLimit(ApewatchWidgetView.LINE_LIMIT).truncationMode(.tail).frame(maxWidth: .infinity, alignment: .leading)
                Image(ApewatchWidgetView.WILDTANGZ_LOGO_NAME).resizable().scaledToFit().frame(maxHeight: ApewatchWidgetView.WILDTANGZ_LOGO_HEIGHT, alignment: .trailing)
            }
            
            Spacer()
            
            if entry.numRequiredAssets < AppAuthorization.REQUIRED_FOR_PORTFOLIO {
                VStack {
                    Text("Wallet does not have \(AppAuthorization.REQUIRED_FOR_PORTFOLIO) \(AppAuthorization.REQUIRED_NAME)")
                }
            } else if family == .systemSmall {
                ApewatchWidgetView.getPortfolioVStack(valueAda: valueAda, valueUsd: valueUsd)
                Spacer()
                ApewatchWidgetView.getRefreshCounter()
            } else {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), alignment: .bottom), count: 2)) {
                    ApewatchWidgetView.getPortfolioVStack(valueAda: valueAda, valueUsd: valueUsd)
                    ApewatchWidgetView.getAssetsVStack(numAssets: numAssets, numProjects: numProjects)
                }
                
                Spacer()
                
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), alignment: .bottom), count: 2)) {
                    ApewatchWidgetView.getRefreshCounter()
                    ApewatchWidgetView.getFloorEstimateCaption()
                }
            }
        }.padding().widgetURL(URL(string: "\(AppConstants.WIDGET_DEEPLINK_SCHEME)://\(AppConstants.APEWATCH_PATH)"))
    }
    
    private static func getPortfolioVStack(valueAda: Double, valueUsd: Double) -> some View {
        return VStack(alignment: .leading) {
            let valueAdaStr = ApewatchWidgetView.getFormattedString(value: valueAda, currency: ApewatchWidgetView.ADA_SYMBOL)
            let valueUsdStr = ApewatchWidgetView.getFormattedString(value: valueUsd, currency: ApewatchWidgetView.USD_SYMBOL)
            Text(valueAdaStr).font(.title).foregroundColor(.accentColor)
            Text(valueUsdStr).font(.body).fontWeight(.light)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
    
    private static func getFloorVStack(floorAda: Double, floorUsd: Double) -> some View {
        VStack(alignment: .trailing) {
            let floorAdaStr = ApewatchWidgetView.getFormattedString(value: floorAda, currency: ApewatchWidgetView.ADA_SYMBOL)
            let floorUsdStr = ApewatchWidgetView.getFormattedString(value: floorUsd, currency: ApewatchWidgetView.USD_SYMBOL)
            Text(floorAdaStr).font(.title3).foregroundColor(.gray).italic()
            Text(floorUsdStr).font(.body).fontWeight(.light).foregroundColor(.gray).italic()
        }.frame(maxWidth: .infinity, alignment: .trailing)
    }
    
    private static func getAssetsVStack(numAssets: Int, numProjects: Int) -> some View {
        VStack(alignment: .trailing) {
            Text("\(numAssets) Assets").font(.title3).foregroundColor(.accentColor)
            Text("\(numProjects) Projects").font(.body).fontWeight(.light)
        }.frame(maxWidth: .infinity, alignment: .trailing)
    }
    
    private static func getRefreshCounter() -> some View {
        let components = DateComponents(minute: -0)
        let futureDate = Calendar.current.date(byAdding: components, to: Date())!
        return Text("Updated \(futureDate, style: .relative) ago").font(.caption2).foregroundColor(.gray)
    }
    
    private static func getFloorEstimateCaption() -> some View {
        return Text("Powered by ApeWatch")
                .font(.caption2)
                .foregroundColor(.gray)
                .frame(maxWidth: .infinity, alignment: .trailing)
    }
    
}

struct Widget_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            let iOddestHumanDadEntry : ApewatchEntry = ApewatchEntry(
                date: Date(),
                configuration: ConfigurationIntent(),
                selection: "$iOddestHumanDad"
            )
            let currentContext = WidgetPreviewContext(family: .systemMedium)
            ApewatchWidgetView(entry: iOddestHumanDadEntry).previewContext(currentContext)
        }
    }
}
