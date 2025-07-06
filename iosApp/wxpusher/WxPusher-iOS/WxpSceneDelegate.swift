import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    var window: UIWindow?
    
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }
        let tempWin = UIWindow(windowScene: windowScene)

        let mainTabBarController = MainTabBarController()
//        let mainTabBarController = WxpProfileViewController()
//        let navigationController = UINavigationController(rootViewController: mainTabBarController)
        tempWin.backgroundColor = .systemBackground
        tempWin.rootViewController = mainTabBarController
        tempWin.makeKeyAndVisible()
        self.window = tempWin
                
    }
    
    func sceneDidDisconnect(_ scene: UIScene) {
    }
    
    func sceneDidBecomeActive(_ scene: UIScene) {
    }
    
    func sceneWillResignActive(_ scene: UIScene) {
    }
    
    func sceneWillEnterForeground(_ scene: UIScene) {
    }
    
    func sceneDidEnterBackground(_ scene: UIScene) {
        
    }
} 
