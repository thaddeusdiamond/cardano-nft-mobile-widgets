//
//  ContentView.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/12/22.
//

import SwiftUI
import CoreData
import WidgetKit
import Toast

struct ContentView: View {
    static let BG_COLOR: UInt = 0x3369D0
    
    static let REQUIRED_POLICY : String = "33568ad11f93b3e79ae8dee5ad928ded72adcea719e92108caf1521b"
    static let REQUIRED_NAME : String = "Wild Tangz"
    static let REQUIRED_NUM : Int = 1
    
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    
    @State var newAddress: String = ""
    @State var selectedAddress: String = (UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) ?? "")
    
    func hasRequiredAssets(address: String, policy: String, minRequired: Int) -> Bool {
        var totalCount = 0
        for tokenPolicy in PoolPm.getTokenPolicies(addressOrAsset: address) {
            if tokenPolicy == policy {
                totalCount += 1
            }
        }
        return totalCount >= minRequired
    }
        
    var body: some View {
        ZStack {
            let darkAwareForeground = (colorScheme == .dark) ? Color.black : Color.white
            let darkAwareBackground = (colorScheme == .dark) ? Color.white : Color.black
            Color(ContentView.BG_COLOR).ignoresSafeArea()
            VStack {
                GeometryReader { (geo) in
                    ScrollView {
                        VStack {
                            Text("Cardano NFT Viewer")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(darkAwareForeground)
                                .padding()
                            
                            Text("Instructions")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(darkAwareForeground)
                            let instructions : [String] = [
                                "Enter an address, handle ($), or single asset ID (asset1...) then click 'Update'. The selection will display under 'Current Selection'",
                                "Exit app, perform a 'long press' on the home screen, then hit '+' in the top-left corner to add a widget to the home screen",
                                "Wait patiently as an NFT loads into the widget viewer (1-2 minutes).  The widget will refresh on its own every 15 minutes."
                            ]
                            VStack {
                                ForEach(Array(instructions.enumerated()), id: \.1) { (index, value) in
                                    Text("**\(index + 1).** *\(value)*")
                                        .font(.body)
                                        .foregroundColor(darkAwareForeground)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                            }
                            
                            TextField("Enter handle, address, or asset ID...", text: $newAddress)
                                .font(.title3)
                                .padding()
                                .background(darkAwareForeground)
                                .cornerRadius(8)
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(darkAwareBackground, lineWidth: 1))
                            Button {
                                if self.newAddress.isEmpty {
                                    return
                                }
                                guard hasRequiredAssets(address: self.newAddress, policy: ContentView.REQUIRED_POLICY, minRequired: ContentView.REQUIRED_NUM) else {
                                    Toast.text(
                                        "Address needs at least \(ContentView.REQUIRED_NUM) \(ContentView.REQUIRED_NAME) NFT(s)",
                                        config: ToastConfiguration(displayTime: 20)
                                    ).show()
                                    return
                                }
                                self.selectedAddress = self.newAddress
                                UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.set(self.selectedAddress, forKey: AppConstants.ADDR_KEY)
                                WidgetCenter.shared.reloadTimelines(ofKind: AppConstants.CONFIG_GROUP_NAME)
                                self.newAddress = ""
                            } label: {
                                Text("Update")
                                    .foregroundColor(darkAwareForeground)
                                    .font(.title2)
                                    .padding()
                            }.background(Color.gray)
                             .cornerRadius(8)
                             .overlay(RoundedRectangle(cornerRadius: 8).stroke(darkAwareBackground, lineWidth: 1))
                            
                            Text("Current Selection")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(darkAwareForeground)
                                .padding(.top)
                            Text(selectedAddress)
                                .font(.title3)
                                .foregroundColor(darkAwareForeground)
                            Spacer()
                            
                            let logoImage = UIImage(named: "BannerIcon")!
                            Image(uiImage: logoImage).resizable().scaledToFit().frame(maxWidth: 500)
                        }.padding().frame(minHeight: geo.size.height)
                    }
                }
            }
        }
    }

}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView().environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
    }
}
