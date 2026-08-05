Pod::Spec.new do |spec|
  spec.name = 'WechatOpenSDK-XCFramework'
  spec.version = '2.0.5'
  spec.summary = 'Wechat OpenSDK XCFramework vendored for reproducible builds.'
  spec.description = <<-DESC
    Official Wechat OpenSDK XCFramework vendored in the repository so CI builds
    do not depend on the availability of the upstream download CDN.
  DESC
  spec.homepage = 'https://open.weixin.qq.com/'
  spec.license = {
    :type => 'Copyright',
    :text => 'Copyright 2020 tencent.com. All rights reserved.'
  }
  spec.author = { 'tencent' => 'weixin-open@qq.com' }
  spec.source = {
    :http => 'https://dldir1.qq.com/WechatWebDev/opensdk/XCFramework/OpenSDK2.0.5.zip'
  }
  spec.platform = :ios, '12.0'
  spec.requires_arc = false
  spec.static_framework = true
  spec.vendored_frameworks = 'WechatOpenSDK.xcframework'
  spec.frameworks = 'Security', 'UIKit', 'CoreGraphics', 'WebKit'
  spec.libraries = 'z', 'sqlite3.0', 'c++'
end
