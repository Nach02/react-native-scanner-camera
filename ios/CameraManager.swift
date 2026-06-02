import Foundation

@objc(CameraManager)
class CameraManager: RCTViewManager {

    override func view() -> UIView! {
        return CameraView()
    }

    @objc func takePhoto(_ reactTag: NSNumber) {
        DispatchQueue.main.async {
            guard let view = self.bridge.uiManager.view(forReactTag: reactTag) as? CameraView else { return }
            view.takePhoto()
        }
    }

    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
