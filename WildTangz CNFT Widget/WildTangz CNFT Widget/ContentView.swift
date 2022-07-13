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
    
    @State var newAddress: String = ""
    @State var selectedAddress: String = (UserDefaults(suiteName: "group.wildtangz")!.string(forKey: PersistenceController.ADDR_KEY) ?? "")
    
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
            Color(ContentView.BG_COLOR).ignoresSafeArea()
            VStack {
                VStack {
                    Text("Cardano NFT Viewer")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    TextField("Enter handle, address, or asset ID here...", text: $newAddress)
                        .font(.title3)
                        .padding()
                        .background(.white)
                        .cornerRadius(8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.black, lineWidth: 1))
                    Button {
                        guard hasRequiredAssets(address: self.newAddress, policy: ContentView.REQUIRED_POLICY, minRequired: ContentView.REQUIRED_NUM) else {
                            Toast.text(
                                "Address needs at least \(ContentView.REQUIRED_NUM) \(ContentView.REQUIRED_NAME) NFT(s)",
                                config: ToastConfiguration(displayTime: 20)
                            ).show()
                            return
                        }
                        self.selectedAddress = self.newAddress
                        UserDefaults(suiteName: "group.wildtangz")!.set(self.selectedAddress, forKey: PersistenceController.ADDR_KEY)
                        WidgetCenter.shared.reloadTimelines(ofKind: "group.wildtangz")
                        self.newAddress = ""
                    } label: {
                        Text("Update")
                            .foregroundColor(.white)
                            .font(.title2)
                            .padding()
                            .background(.gray)
                            .cornerRadius(8)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.black, lineWidth: 1))
                    }.padding()
                    
                    Spacer()
                    
                    Text("Current Address Selected")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    Text(selectedAddress)
                        .font(.title3)
                        .foregroundColor(.white)
                        .padding(.top)
                    
                    Spacer()
                }.padding()
                
                Spacer()
                
                let logoImage = UIImage(named: "BannerIcon")!
                Image(uiImage: logoImage).resizable().scaledToFit()
            }
        }
    }

}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView().environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
    }
}
