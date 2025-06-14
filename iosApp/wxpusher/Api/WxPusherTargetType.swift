//
//  WxPusherTargetType.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/28.
//



import Moya
import RxSwift

protocol WxPusherTargetType:TargetType {
    var parameters:Any? { get }
    var deviceId:String{ get }
    var deviceToken:String{ get }
    
}


extension WxPusherTargetType {
    var headers: [String: String]? {
        return [
            "Content-type": "application/json",
            "platform":"iOS",
            "deviceToken":deviceToken,
            "deviceUuid":deviceId,
            "version":version()
        ]
    }
    
    var baseURL: URL {
        return URL(string: "https://wxpusher.zjiecode.com")!
    }
    
    var parameterEncoding: ParameterEncoding {
        return URLEncoding.default
    }
    
    static var provider: RxSwift.Reactive<MoyaProvider<Self>> {
        return self.weakProvider
    }
    
    static var weakProvider: RxSwift.Reactive<MoyaProvider<Self>> {
        var plugins: [PluginType] = []
        #if DEBUG
        plugins.append(LogPlugin())
        #endif
        let provider = MoyaProvider<Self>(plugins: plugins)
        return provider.rx
    }
}

///将返回和Rx组合
 public extension RxSwift.Reactive where Base: MoyaProviderType {
     func http<T>(_ token: Base.Target, callbackQueue: DispatchQueue? = nil) -> Observable<BaseResp<T>> {
        return Single.create { [weak base] single in
            let cancellableToken = base?.request(token, callbackQueue: callbackQueue, progress: nil) { result in
                switch result {
                case let .success(response):
                    do{
                        let data = response.data
                        print("resp="+(String(data: data, encoding: .utf8) ?? ""))
//                        let respData:BaseResp = try JSONSerialization.jsonObject(with: data) as! BaseResp<T>;
                        let respData:BaseResp = try JSONDecoder().decode(BaseResp<T>.self, from:data)
                        let statusCode = response.statusCode
                        if (statusCode != 200) {
                            single(.failure(NetError(code: response.statusCode, msg: response.description,cause: MoyaError.statusCode(response))))
                        }else if(respData.code != 1000){
                            single(.failure(NetError(code: respData.code, msg: respData.msg)))
                        }else{
                            single(.success(respData))
                        }
                    }catch{
                        single(.failure(NetError(code: 0, msg: "数据解析错误")))
                    }
                    
                case let .failure(error):
                    single(.failure(NetError(code: 0, msg: error.localizedDescription,cause: error)))
                }
            }
            
            return Disposables.create {
                cancellableToken?.cancel()
            }
        }.asObservable()
    }
}

///打印请求日志的插件
private class LogPlugin: PluginType {
    func willSend(_ request: RequestType, target: TargetType) {
        print("\n-------------------\n请求参数: \(target.path)")
        print("请求方式: \(target.method.rawValue)")
        if let params = (target as? WxPusherTargetType)?.parameters {
            print(params)
        }
        print("\n")
    }

    func didReceive(_ result: Result<Response, MoyaError>, target: TargetType) {
        print("\n-------------------\n响应参数: \(target.path)")
        if let data = try? result.get().data, let resutl = String(data: data, encoding: String.Encoding.utf8) {
            print("请求结果: \(resutl)")
        }
        print("\n")
    }
}
