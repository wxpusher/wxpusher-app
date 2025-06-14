import Foundation
import Moya

enum MessageService {
    case getMessages(page: Int)
}

extension MessageService: TargetType {
    var baseURL: URL {
        return URL(string: "http://wxpusher.zjiecode.com/api")!
    }
    
    var path: String {
        switch self {
        case .getMessages:
            return "/message/list"
        }
    }
    
    var method: Moya.Method {
        switch self {
        case .getMessages:
            return .get
        }
    }
    
    var task: Task {
        switch self {
        case .getMessages(let page):
            return .requestParameters(parameters: ["page": page], encoding: URLEncoding.default)
        }
    }
    
    var headers: [String : String]? {
        return ["Content-Type": "application/json"]
    }
}

//struct MessageListItem: Codable {
//    let qid: String
//    let summary: String
//    let url: String?
//    let name: String
//}
//
//struct MessageListResponse: Codable {
//    let code: Int
//    let msg: String
//    let data: [MessageListItem]
//} 
