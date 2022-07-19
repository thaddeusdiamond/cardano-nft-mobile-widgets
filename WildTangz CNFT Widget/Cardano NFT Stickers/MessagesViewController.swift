//
//  MessagesViewController.swift
//  Cardano NFT Stickers
//
//  Created by Thaddeus Diamond on 7/17/22.
//

import UIKit
import Messages

class MessagesViewController: MSMessagesAppViewController {
    
    var browserViewController : NFTStickersBrowserViewController!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        browserViewController = NFTStickersBrowserViewController(stickerSize: .regular)
        browserViewController.view.frame = self.view.frame
        
        self.addChild(browserViewController)
        browserViewController.didMove(toParent: self)
        self.view.addSubview(browserViewController.view)
        
        browserViewController.loadStickers()
        browserViewController.stickerBrowserView.reloadData()
    }
  
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
      
        browserViewController.view.frame = self.view.frame
    }
  
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
}
