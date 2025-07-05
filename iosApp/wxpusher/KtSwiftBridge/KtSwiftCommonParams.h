//
//  KtSwiftCommonParams.h
//  wxpusher
//
//  Created by zjie on 2025/6/23.
//

#ifndef KtSwiftCommonParams_h
#define KtSwiftCommonParams_h
#import <Foundation/Foundation.h>

/**
 * 部分时候，需要kotlin能够调用到iOS壳工程的实现，在这里声明成接口，kt编译的时候，会生成接口文件。
 */
@interface KtSwiftCommonParams : NSObject

+ (NSString*) appVersionName;

@end


#endif 
