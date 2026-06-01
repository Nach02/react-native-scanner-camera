#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(ScannerManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(onScan, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(codeTypes, NSArray)
@end
