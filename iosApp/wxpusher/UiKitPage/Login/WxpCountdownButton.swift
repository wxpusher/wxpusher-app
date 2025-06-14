//
//  WxpCountdownButton.swift
//  wxpusher
//
//  Created by zjie on 2025/6/8.
//

import UIKit
import SwiftUI

class WxpCountdownButton: UIButton {
    private var countdownTimer: Timer?
    private var remainingSeconds: Int = 0
    
    var normalTitle: String = "获取验证码"
    
    func startCountdown(seconds: Int) {
        remainingSeconds = seconds
        isEnabled = false
        
        updateTitle()
        backgroundColor = UIColor.defAccentSecoundColor
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(
            timeInterval: 1,
            target: self,
            selector: #selector(updateCountdown),
            userInfo: nil,
            repeats: true
        )
    }
    
    @objc private func updateCountdown() {
        remainingSeconds -= 1
        updateTitle()
        
        if remainingSeconds <= 0 {
            stopCountdown()
        }
    }
    
    private func updateTitle() {
        if remainingSeconds > 0 {
            setTitle("\(remainingSeconds)S", for: .normal)
        } else {
            setTitle(normalTitle, for: .normal)
        }
    }
    
    func stopCountdown() {
        countdownTimer?.invalidate()
        countdownTimer = nil
        isEnabled = true
        backgroundColor = UIColor.defAccentPrimaryColor
        setTitle(normalTitle, for: .normal)
    }
    
    deinit {
        countdownTimer?.invalidate()
    }
}
