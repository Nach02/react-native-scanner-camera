#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(CameraManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(onReady, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoTaken, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoError, RCTBubblingEventBlock)
@end
