#import <React/RCTViewManager.h>
#import <React/RCTUIManager.h>

@interface RCT_EXTERN_MODULE(CameraManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(onReady, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoTaken, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoError, RCTBubblingEventBlock)
RCT_EXTERN_METHOD(takePhoto:(nonnull NSNumber *)reactTag)
@end
