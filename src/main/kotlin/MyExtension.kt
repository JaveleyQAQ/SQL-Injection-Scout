import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.extension.ExtensionUnloadingHandler
import burp.api.montoya.persistence.PersistedObject
import burp.api.montoya.persistence.Persistence
import config.Configs
import config.DataPersistence
import controller.HttpInterceptor
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import ui.components.LogViewPanel
import ui.components.SettingPanel

//import ui.components.MyHttpRequestEditorProvider

class MyExtension : BurpExtension, ExtensionUnloadingHandler {
    private lateinit var api: MontoyaApi
    private lateinit var logs: LogEntry
    private lateinit var modifiedLog: ModifiedLogEntry
    private lateinit var httpInterceptor: HttpInterceptor
    private lateinit var dataPersistence: DataPersistence

    override fun initialize(api: MontoyaApi) {
        this.api = api
        val configs = Configs.INSTANCE
        api.extension().setName("${configs.extensionName}\uD83D\uDE2D")

        // 初始化顺序很重要
        dataPersistence = DataPersistence(api)  // 先初始化数据持久化
        logs = LogEntry(api)
        modifiedLog = ModifiedLogEntry(logs)
        httpInterceptor = HttpInterceptor(logs, api, modifiedLog)

        // 注册UI组件
        api.userInterface().registerSuiteTab("SQL Scout \uD83D\uDE2D", LogViewPanel(api, logs, modifiedLog, httpInterceptor,dataPersistence).buildUI())

        // 注册HTTP处理器和UI
        api.http().registerHttpHandler(httpInterceptor)

        api.logging().logToOutput(
            """
            [#] ${configs.extensionName}
            [#] Author: JaveleyQAQ
            [#] Github: https://github.com/JaveleyQAQ
            [#] Version: ${Configs.INSTANCE.version}
            """.trimIndent()
        )
    }
    override fun extensionUnloaded() {
//        extensionUnloaded()
        Runtime.getRuntime().addShutdownHook(Thread {
            ExecutorManager.get().shutdown()
        })
        api.logging().logToOutput("Extension was unloaded.");
    }

    fun saveConfig(config: Configs){
        val extensionData = api.persistence().extensionData()
//        val p = PersistedObject.persistedObject().set
//        extensionData.setChildObject()


    }
}

