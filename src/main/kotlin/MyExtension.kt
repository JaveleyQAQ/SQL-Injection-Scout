import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.extension.ExtensionUnloadingHandler
import config.Configs
import config.DataPersistence
import example.contextmenu.SiteMapContextMenuItemsProvider
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import processor.http.HttpInterceptor
import ui.components.LogViewPanel


class MyExtension : BurpExtension, ExtensionUnloadingHandler{
    private lateinit var api: MontoyaApi
    private lateinit var logs: LogEntry
    private lateinit var modifiedLog: ModifiedLogEntry
    private lateinit var httpInterceptor: HttpInterceptor
    private lateinit var dataPersistence: DataPersistence
    private lateinit var logViewPanel: LogViewPanel

    override fun initialize(api: MontoyaApi) {
        this.api = api
        val configs = Configs.INSTANCE
        api.extension().setName("${configs.extensionName}")

        dataPersistence = DataPersistence(api)  // 先初始化数据持久化
        logs = LogEntry(api)
        modifiedLog = ModifiedLogEntry(logs)
        httpInterceptor = HttpInterceptor(logs, api, modifiedLog)
        logViewPanel = LogViewPanel(api, logs, modifiedLog, httpInterceptor,dataPersistence)

        // 注册HTTP处理器和UI
        api.userInterface().registerSuiteTab("SQL Scout",logViewPanel.buildUI() )
        api.http().registerHttpHandler(httpInterceptor)
        api.userInterface().registerContextMenuItemsProvider(SiteMapContextMenuItemsProvider(api, httpInterceptor))

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

