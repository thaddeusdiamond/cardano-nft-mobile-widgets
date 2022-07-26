//
//  NftInfo.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/21/22.
//

import Foundation
import SVGView
import SwiftUI

class NftInfo {
    
    static let SVG_IMAGE_TYPE = "image/svg+xml"
    static let SVG_SETTINGS = SVGSettings(ppi: 48)
    
    static let DATA_PREFIX = "data:"
    
    static let MAX_PIXEL_SIZE = 350
    static let DOWNSAMPLE_OPTS: [NSString: Any]  = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceShouldCache: true,
        kCGImageSourceShouldCacheImmediately: false,
        kCGImageSourceThumbnailMaxPixelSize: NftInfo.MAX_PIXEL_SIZE
    ]
    
    let mediaType: String?
    let uiImage: UIImage?
    let svgNode: SVGNode?
    
    init(mediaType: String? = nil, imageData: Data? = nil, imageUrl: String? = nil) {
        self.mediaType = mediaType
        let isIpfs = imageUrl != nil ? (URL(string: imageUrl!)?.scheme == nil) : false
        self.uiImage = NftInfo.asUIImage(mediaType: mediaType, imageData: imageData, imageUrl: imageUrl, isIpfs: isIpfs)
        self.svgNode = NftInfo.asSVGNode(mediaType: mediaType, imageData: imageData, imageUrl: imageUrl, isIpfs: isIpfs)
    }
    
    private static func asUIImage(mediaType: String?, imageData: Data?, imageUrl: String?, isIpfs: Bool) -> UIImage? {
        guard mediaType != NftInfo.SVG_IMAGE_TYPE else {
            return nil
        }
        
        var imageRawData : Data? = imageData
        if imageUrl != nil, isIpfs {
            imageRawData = Blockfrost.getDataFromIpfs(imageUrl!)
        } else if imageUrl != nil {
            imageRawData = try? Data(contentsOf: URL(string: imageUrl!)!)
        }
        
        if imageRawData != nil {
            return NftInfo.downsampledImage(imageData: imageRawData!)
        }
        return nil
    }
    
    private static func downsampledImage(imageData: Data) -> UIImage? {
        let imageSourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
        guard let imageSource = CGImageSourceCreateWithData(imageData as CFData, imageSourceOptions),
              let scaledImage = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, NftInfo.DOWNSAMPLE_OPTS as CFDictionary) else {
            return nil
        }
        
        return UIImage(cgImage: scaledImage)
    }
    
    private static func asSVGNode(mediaType: String?, imageData: Data?, imageUrl: String?, isIpfs: Bool) -> SVGNode? {
        guard mediaType == NftInfo.SVG_IMAGE_TYPE else {
            return nil
        }
        var imageRawData : Data? = imageData
        if imageUrl != nil, isIpfs {
            imageRawData = Blockfrost.getDataFromIpfs(imageUrl!)
        } else if imageUrl != nil {
            imageRawData = try? Data(contentsOf: URL(string: imageUrl!)!)
        }
        
        if imageRawData == nil {
            return nil
        }
        
        return SVGParser.parse(data: imageRawData!, settings: NftInfo.SVG_SETTINGS)
    }
    
}
