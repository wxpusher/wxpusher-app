//
//  UIColor+Color.swift
//  wxpusher
//
//  Created by zjie on 2025/6/8.
//

import UIKit

extension UIColor {
    
    //主要主题色
    public static let defAccentPrimaryColor: UIColor = UIColor(named: "AccentColor")!
    //次要主题色
    public static let defAccentSecoundColor: UIColor = UIColor(named: "AccentSecoundColor")!
    
    public static let defFontPrimaryColor: UIColor = UIColor(named: "FontPrimaryColor")!
    
    public static let defFontSecondColor: UIColor = UIColor(named: "FontSecondColor")!
    
    public static let defDividerSecoundColor: UIColor = UIColor(named: "DividerSecoundColor")!
//
//    public static let defFontReversalPrimaryColor: Color = Color("FontReversalPrimaryColor")
//    
//    public static let defFontSecondColor: Color = Color("FontSecondColor")
//    
//    public static let defFontThirdColor: Color = Color("FontThirdColor")
    
    
//    init(hex: String,opacity: Double) {
//        let scanner = Scanner(string: hex)
//        _ = scanner.scanString("#")
//
//        var rgb: UInt64 = 0
//
//        scanner.scanHexInt64(&rgb)
//
//        let red = Double((rgb & 0xFF0000) >> 16) / 255.0
//        let green = Double((rgb & 0x00FF00) >> 8) / 255.0
//        let blue = Double(rgb & 0x0000FF) / 255.0
//        self.init(red: red, green: green, blue: blue,opacity: opacity)
//    }
}
