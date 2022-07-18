//
//  WildTangz_CNFT_WidgetApp.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/12/22.
//

import SwiftUI
import WidgetKit

@main
struct WildTangz_CNFT_WidgetApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                    WidgetCenter.shared.reloadTimelines(ofKind: "group.wildtangz")
                }
        }
    }
}
