//
//  ColorExt.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/4.
//

import SwiftUI

extension Color {
    
    //主要主题色
    public static let defAccentPrimaryColor: Color = Color("AccentColor")
    //次要主题色
    public static let defAccentSecoundColor: Color = Color("AccentSecoundColor")
    
    public static let defFontPrimaryColor: Color = Color("FontPrimaryColor")
    
    public static let defFontReversalPrimaryColor: Color = Color("FontReversalPrimaryColor")
    
    public static let defFontSecondColor: Color = Color("FontSecondColor")
    
    public static let defFontThirdColor: Color = Color("FontThirdColor")
    
    
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
