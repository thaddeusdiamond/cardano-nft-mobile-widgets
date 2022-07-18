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
    
}
