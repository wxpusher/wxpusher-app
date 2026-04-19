//
//  KtSwiftDialogUtils.m
//  WxPusher-iOS
//
//  Created by zjie on 2025/8/30.
//

#import "KtSwiftDialogUtils.h"
#import "WxPusher_iOS-Swift.h"

@implementation WxpDialogParams

- (instancetype)initWithTitle:(NSString *)title
                     message:(NSString *)message
                    leftText:(NSString *)leftText
                   leftBlock:(WxpBlockNoParamNoReturn)leftBlock
                   rightText:(NSString *)rightText
                  rightBlock:(WxpBlockNoParamNoReturn)rightBlock
                  cancelable:(BOOL)cancelable {
    self = [super init];
    if (self) {
        _title = title;
        _message = message;
        _leftText = leftText;
        _leftBlock = leftBlock;
        _rightText = rightText;
        _rightBlock = rightBlock;
        _cancelable = cancelable;
    }
    return self;
}

// 默认初始化方法
- (instancetype)init {
    return [self initWithTitle:nil message:nil leftText:nil leftBlock:nil rightText:nil rightBlock:nil cancelable:YES];
}

- (nonnull instancetype)initWithTitle:(nullable NSString *)title rightText:(nullable NSString *)rightText {
    return [self initWithTitle:title message:nil leftText:nil leftBlock:nil rightText:rightText rightBlock:nil cancelable:YES];
}

@end

@implementation KtSwiftDialogUtils

+ (void)showDialog:(WxpDialogParams *)params{
    [WxpDialogUtils showDialogWithParams:params];
}

@end
