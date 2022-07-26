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
    
    static let MAX_PIXEL_SIZE = 360
    static let DOWNSAMPLE_OPTS: [NSString: Any]  = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceShouldCache: true,
        kCGImageSourceShouldCacheImmediately: false,
        kCGImageSourceThumbnailMaxPixelSize: NftInfo.MAX_PIXEL_SIZE
        //kCGImageSourceSubsampleFactor: 8
    ]
    
    let mediaType: String?
    let uiImage: UIImage?
    let svgNode: SVGNode?
    
    init(mediaType: String? = nil, imageData: Data? = nil, imageUrl: URL? = nil) {
        self.mediaType = mediaType
        self.uiImage = NftInfo.asUIImage(mediaType: mediaType, imageData: imageData, imageUrl: imageUrl)
        self.svgNode = NftInfo.asSVGNode(mediaType: mediaType, imageData: imageData, imageUrl: imageUrl)
    }
    
    private static func asUIImage(mediaType: String?, imageData: Data?, imageUrl: URL?) -> UIImage? {
        guard mediaType != NftInfo.SVG_IMAGE_TYPE else {
            return nil
        }
        if imageData != nil {
            return UIImage(data: imageData!)
        } else if imageUrl != nil {
            return NftInfo.downsampledImage(imageUrl: imageUrl!)
        }
        return nil
    }
    
    private static func downsampledImage(imageUrl: URL) -> UIImage? {
        let imageSourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
        guard let imageSource = CGImageSourceCreateWithURL(imageUrl as CFURL, imageSourceOptions),
              let scaledImage = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, NftInfo.DOWNSAMPLE_OPTS as CFDictionary) else {
            return nil
        }
        
        return UIImage(cgImage: scaledImage)
    }
    
    private static func asSVGNode(mediaType: String?, imageData: Data?, imageUrl: URL?) -> SVGNode? {
        guard mediaType == NftInfo.SVG_IMAGE_TYPE else {
            return nil
        }
        if imageData != nil {
            return SVGParser.parse(data: imageData!, settings: NftInfo.SVG_SETTINGS)
        } else if imageUrl != nil {
            return SVGParser.parse(contentsOf: imageUrl!, settings: NftInfo.SVG_SETTINGS)
        }
        return nil
    }
    
}
