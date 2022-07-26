//
//  Utils.swift
//  WildTangz CNFT Widget
//
//  Created by Thaddeus Diamond on 7/24/22.
//

import Foundation
import SwiftyJSON

let GET_REQUEST = "GET"

func getAsJson(url: String, method: String = GET_REQUEST, headers: [String:String] = [:]) throws -> JSON {
    guard let apiEndpoint : URL = URL(string: url) else {
        return JSON()
    }
    
    let urlData = try getAsData(url: apiEndpoint, method: method, headers: headers)
    if let metadataData : Data = urlData {
        return try JSON(data: metadataData)
    }
    return JSON()
}

func getAsData(url: URL, method: String = GET_REQUEST, headers: [String:String] = [:]) throws -> Data? {
    var request = URLRequest(url: url)
    request.httpMethod = method
    for (headerField, value) in headers {
        request.addValue(value, forHTTPHeaderField: headerField)
    }
    
    let semaphore = DispatchSemaphore(value: 0)
    var urlData : Data?
    let task = URLSession.shared.dataTask(with: request) { data, response, error in
        defer {
            semaphore.signal()
        }
        urlData = data
    }
    task.resume()
    _ = semaphore.wait(timeout: .distantFuture)
    return urlData
}
