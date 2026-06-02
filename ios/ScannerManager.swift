import Foundation
import React

@objc(ScannerManager)
class ScannerManager: RCTViewManager {

    override func view() -> UIView! {
        return ScannerView()
    }

    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
