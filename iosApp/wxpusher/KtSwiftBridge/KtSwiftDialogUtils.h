//
//  KtSwiftDialogUtils.h
//  WxPusher-iOS
//
//  Created by zjie on 2025/8/30.
//

#ifndef KtSwiftDialogUtils_h
#define KtSwiftDialogUtils_h

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

// 定义无参数无返回值的 Block 类型
typedef void (^WxpBlockNoParamNoReturn)(void);

@interface WxpDialogParams : NSObject

@property (nonatomic, copy, nullable) NSString *title;
@property (nonatomic, copy, nullable) NSString *message;
@property (nonatomic, copy, nullable) NSString *leftText;
@property (nonatomic, copy, nullable) WxpBlockNoParamNoReturn leftBlock;
@property (nonatomic, copy, nullable) NSString *rightText;
@property (nonatomic, copy, nullable) WxpBlockNoParamNoReturn rightBlock;

// 可选：便利初始化方法
- (instancetype)initWithTitle:(nullable NSString *)title
                     message:(nullable NSString *)message
                    leftText:(nullable NSString *)leftText
                   leftBlock:(nullable WxpBlockNoParamNoReturn)leftBlock
                   rightText:(nullable NSString *)rightText
                  rightBlock:(nullable WxpBlockNoParamNoReturn)rightBlock;

//一般用于【我知道了】
- (instancetype)initWithTitle:(nullable NSString *)title
                    rightText:(nullable NSString *)rightText;


@end


@interface KtSwiftDialogUtils : NSObject


+ (void) showDialog:(WxpDialogParams*) params;

@end


NS_ASSUME_NONNULL_END
#endif
