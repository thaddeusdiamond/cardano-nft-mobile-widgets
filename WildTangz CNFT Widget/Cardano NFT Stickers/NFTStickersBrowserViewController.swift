//
//  NFTStickersBrowserViewController.swift
//  Cardano NFT Stickers
//
//  Created by Thaddeus Diamond on 7/18/22.
//

import Foundation
import Messages

class NFTStickersBrowserViewController : MSStickerBrowserViewController {
    
    var stickers = [MSSticker]()
    
    override func numberOfStickers(in stickerBrowserView: MSStickerBrowserView) -> Int {
        return stickers.count
    }
    
    override func stickerBrowserView(_ stickerBrowserView: MSStickerBrowserView, stickerAt index: Int) -> MSSticker {
        return stickers[index]
    }
    
    func loadStickers() {
        guard let address = UserDefaults(suiteName: AppConstants.CONFIG_GROUP_NAME)!.string(forKey: AppConstants.ADDR_KEY) else {
            return
        }
        
        if let sticker = createSticker(asset: address) {
            stickers.append(sticker)
        }
    }
    
    private func createSticker(asset: String) -> MSSticker? {
        do {
            if let imageData : Data = PoolPm.getNftFromAddrString(addressOrAsset: asset)?.imageData {
                let documentDirPath : URL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                let stickerFile : URL = documentDirPath.appendingPathComponent(asset)
                try imageData.write(to: stickerFile)
                
                return try MSSticker(contentsOfFileURL: stickerFile, localizedDescription: asset)
            }
        } catch {
            // Do nothing, an error occurred
        }
        return nil
    }
    
}
