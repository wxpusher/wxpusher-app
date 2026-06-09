//
//  WxpBannerAdView.swift
//  wxpusher
//
//  穿山甲 Banner（模版渲染 / Express）广告封装。
//  代码位 ID 写死在本类中（消息详情页底部 Banner）。
//

import UIKit
import BUAdSDK
import shared

protocol WxpBannerAdViewDelegate: AnyObject {
    /// 模版渲染成功，host 据此展开容器到 height 高度
    func bannerAdDidRender(_ adView: WxpBannerAdView, height: CGFloat)
    /// 加载或渲染失败，host 据此保持/收起隐藏
    func bannerAdDidFail(_ adView: WxpBannerAdView)
    /// 用户关闭（dislike），host 据此收起隐藏
    func bannerAdDidClose(_ adView: WxpBannerAdView)
}

final class WxpBannerAdView: UIView {

    /// 消息详情页底部 Banner 代码位 ID
    static let messageDetailSlotID = "983456094"

    /// Banner 模版比例（宽:高 = 600:150）
    private static let ratio: CGFloat = 150.0 / 600.0

    weak var delegate: WxpBannerAdViewDelegate?

    private weak var rootViewController: UIViewController?
    private var bannerView: BUNativeExpressBannerView?

    init(rootViewController: UIViewController) {
        self.rootViewController = rootViewController
        super.init(frame: .zero)
        backgroundColor = .clear
        clipsToBounds = true
        translatesAutoresizingMaskIntoConstraints = false
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    /// 按当前可用宽度加载一条 Banner 广告
    func loadAd(slotID: String, width: CGFloat) {
        guard let rootViewController = rootViewController, width > 0 else {
            delegate?.bannerAdDidFail(self)
            return
        }
        let height = (width * WxpBannerAdView.ratio).rounded()
        let adSize = CGSize(width: width, height: height)

        let banner = BUNativeExpressBannerView(
            slotID: slotID,
            rootViewController: rootViewController,
            adSize: adSize
        )
        banner.frame = CGRect(x: 0, y: 0, width: width, height: height)
        banner.delegate = self
        addSubview(banner)
        bannerView = banner
        // 按当前界面深/浅色设置广告主题，使创意与页面背景协调
        WxpPangleAdManager.shared.applyAdTheme(for: rootViewController.traitCollection)
        banner.loadAdData()
    }
}

extension WxpBannerAdView: BUNativeExpressBannerViewDelegate {

    func nativeExpressBannerAdViewDidLoad(_ bannerAdView: BUNativeExpressBannerView) {
        // 广告数据拉取成功，等待渲染回调
    }

    func nativeExpressBannerAdView(_ bannerAdView: BUNativeExpressBannerView, didLoadFailWithError error: Error?) {
        WxpLogUtils.shared.d(tag: "WxpAd", message: "Banner拉取失败: \(error?.localizedDescription ?? "")", throwable: nil)
        delegate?.bannerAdDidFail(self)
    }

    func nativeExpressBannerAdViewRenderSuccess(_ bannerAdView: BUNativeExpressBannerView) {
        let renderedHeight = bannerAdView.bounds.height > 0 ? bannerAdView.bounds.height : bannerAdView.frame.height
        bannerAdView.frame = CGRect(x: 0, y: 0, width: bounds.width, height: renderedHeight)
        delegate?.bannerAdDidRender(self, height: renderedHeight)
    }

    func nativeExpressBannerAdViewRenderFail(_ bannerAdView: BUNativeExpressBannerView, error: Error?) {
        WxpLogUtils.shared.d(tag: "WxpAd", message: "Banner渲染失败: \(error?.localizedDescription ?? "")", throwable: nil)
        delegate?.bannerAdDidFail(self)
    }

    func nativeExpressBannerAdViewDidClick(_ bannerAdView: BUNativeExpressBannerView) {
        // 点击广告，无需额外处理
    }

    func nativeExpressBannerAdView(_ bannerAdView: BUNativeExpressBannerView, dislikeWithReason filterWords: [BUDislikeWords]?) {
        delegate?.bannerAdDidClose(self)
    }
}
