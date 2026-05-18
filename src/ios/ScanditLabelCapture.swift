import ScanditFrameworksCore
import ScanditFrameworksLabel

#if SWIFT_PACKAGE
import Cordova
import ScanditCordovaDatacaptureCore
#endif

@objc(ScanditLabelCapture)
public class ScanditLabelCapture: CDVPlugin {
    var labelModule: LabelCaptureModule!
    var emitter: CordovaEventEmitter!

    override public func pluginInitialize() {
        super.pluginInitialize()
        emitter = CordovaEventEmitter(commandDelegate: commandDelegate)
        labelModule = LabelCaptureModule(emitter: emitter)
        labelModule.didStart()
    }

    public override func dispose() {
        labelModule.didStop()
        emitter.removeCallbacks()
        super.dispose()
    }

    @objc(executeLabel:)
    func executeLabel(_ command: CDVInvokedUrlCommand) {
        guard let argsJson = command.defaultArgumentAsDictionary else {
            commandDelegate.send(
                .failure(with: "Invalid argument received in executeLabel"),
                callbackId: command.callbackId
            )
            return
        }

        let coreModuleName = String(describing: CoreModule.self)
        guard let coreModule = DefaultServiceLocator.shared.resolve(clazzName: coreModuleName) as? CoreModule else {
            commandDelegate.send(
                .failure(with: "Unable to retrieve the CoreModule from the locator."),
                callbackId: command.callbackId
            )
            return
        }

        let result = CordovaResult(commandDelegate, emitter: emitter, command: command)
        let handled = coreModule.execute(
            CordovaMethodCall(command: command),
            result: result,
            module: self.labelModule
        )

        if !handled {
            let methodName = argsJson["methodName"] as? String ?? "unknown"
            commandDelegate.send(.failure(with: "Unknown Core method: \(methodName)"), callbackId: command.callbackId)
        }
    }

    @objc(getDefaults:)
    func getDefaults(command: CDVInvokedUrlCommand) {
        let defaults = labelModule.getDefaults()
        let defaultsResult = ["LabelCapture": defaults]
        commandDelegate.send(
            .success(message: defaultsResult as CDVPluginResult.JSONMessage),
            callbackId: command.callbackId
        )
    }
}
