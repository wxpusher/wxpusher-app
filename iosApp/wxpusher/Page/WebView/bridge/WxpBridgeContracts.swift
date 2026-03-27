import Foundation
import shared

struct WxpBridgeRequest {
    let action: String
    let data: [String: Any]
    let callbackId: String?
}

struct WxpBridgeResponse {
    let success: Bool
    let data: [String: Any]?
    let error: String?

    static func ok(_ data: [String: Any]? = nil) -> WxpBridgeResponse {
        WxpBridgeResponse(success: true, data: data, error: nil)
    }

    static func fail(_ error: String) -> WxpBridgeResponse {
        WxpBridgeResponse(success: false, data: nil, error: error)
    }
}

typealias WxpBridgeCompletion = (WxpBridgeResponse) -> Void

protocol WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter)
}

extension WxpBridgeActionHandler {
    func buildDictionary(from object: Any) -> [String: Any] {
        if isKmpModel(of: type(of: object)) {
            let jsonStr = WxpSerializationUtils.shared.toJson(value: object, serializer: WxpLoginInfo.companion.serializer())
            guard let jsonData = jsonStr?.data(using: .utf8) else {
                return [:]
            }

            do {
                let json = try JSONSerialization.jsonObject(with: jsonData, options: [])
                return json as? [String: Any] ?? [:]
            } catch {
                return [:]
            }
        }
        guard let value = normalizeBridgeValue(object) else {
            return [:]
        }
        return value as? [String: Any] ?? [:]
    }

    private func isKmpModel(of type: Any.Type) -> Bool {
        let fullTypeName = String(reflecting: type)
        if fullTypeName.hasPrefix("Shared") {
            return true
        }

        if let cls = type as? AnyClass,
           let kotlinBaseClass = NSClassFromString("KotlinBase"),
           cls.isSubclass(of: kotlinBaseClass) {
            return true
        }

        return false
    }

    private func normalizeBridgeValue(_ value: Any) -> Any? {
        let mirror = Mirror(reflecting: value)

        if mirror.displayStyle == .optional {
            guard let (_, someValue) = mirror.children.first else {
                return nil
            }
            return normalizeBridgeValue(someValue)
        }

        if let displayStyle = mirror.displayStyle {
            switch displayStyle {
            case .collection, .set:
                return mirror.children.compactMap { normalizeBridgeValue($0.value) }
            case .dictionary:
                var dict: [String: Any] = [:]
                for child in mirror.children {
                    let pairMirror = Mirror(reflecting: child.value)
                    let pairValues = Array(pairMirror.children.map(\.value))
                    if pairValues.count == 2,
                       let key = normalizeBridgeValue(pairValues[0]),
                       let val = normalizeBridgeValue(pairValues[1]) {
                        dict[String(describing: key)] = val
                    }
                }
                return dict
            case .struct, .class:
                var dict: [String: Any] = [:]
                for child in mirror.children {
                    guard let key = child.label,
                          let val = normalizeBridgeValue(child.value) else {
                        continue
                    }
                    dict[key] = val
                }
                return dict
            case .enum:
                return String(describing: value)
            case .tuple:
                return mirror.children.compactMap { normalizeBridgeValue($0.value) }
            default:
                break
            }
        }

        if value is NSString || value is NSNumber || value is NSNull || value is Date {
            return value
        }
        if value is String || value is Int || value is Double || value is Float || value is Bool {
            return value
        }
        return String(describing: value)
    }
}

struct WxpRegisteredBridgeHandler {
    let requiresWhitelist: Bool
    let handler: WxpBridgeActionHandler
}
