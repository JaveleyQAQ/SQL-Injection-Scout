package config

import burp.api.montoya.MontoyaApi
import burp.api.montoya.persistence.PersistedList
import burp.api.montoya.persistence.PersistedObject

/**
 * 数据持久化
 */
class DataPersistence(val api: MontoyaApi) {
    private var persistenceData: PersistedObject = this.api.persistence().extensionData()!!
    private val JAVELEYFLAG = "JAVELEYFLAG"
    val config = Configs.INSTANCE

    init {
        if (persistenceData.getString(JAVELEYFLAG) == null) {
            // 首次加载，初始化持久化数据
            persistenceData.setString(JAVELEYFLAG, JAVELEYFLAG)
            setData()
        } else {
            // 非首次加载，读取持久化数据
            loadData()
        }
    }

    fun loadData() {
        // 加载基本配置
        config.startUP = persistenceData.getBoolean("startUP") ?: true
        config.isInScope = persistenceData.getBoolean("isInScope") ?: true
        config.proxy = persistenceData.getBoolean("proxy") ?: true
        config.repeater = persistenceData.getBoolean("repeater") ?: true
        config.nullCheck = persistenceData.getBoolean("nullCheck") ?: true

        // 加载其他数值类型配置
        config.maxAllowedParameterCount = persistenceData.getInteger("maxAllowedParameterCount") ?: 30
        config.randomCheckTimer = persistenceData.getLong("randomCheckTimer") ?: 5000
        config.fixedIntervalTime = persistenceData.getLong("fixedIntervalTime") ?: 300
        config.neverScanRegex = persistenceData.getString("neverScanRegex") ?: "(delete|del)"

        // 加载列表类型配置
        config.payloads = persistenceData.getStringList("payloads") ?: Configs.INSTANCE.payloads
        config.heuristicWordsError = persistenceData.getStringList("heuristicWordsError") ?: Configs.INSTANCE.heuristicWordsError
        config.uninterestingType = persistenceData.getStringList("uninterestingType")?:Configs.INSTANCE.uninterestingType
        config.allowedMimeTypeMimeType = persistenceData.getStringList("allowedMimeTypeMimeType")?:Configs.INSTANCE.allowedMimeTypeMimeType
    }

    private fun setData() {
        // 保存基本配置
        persistenceData.setBoolean("startUP", config.startUP)
        persistenceData.setBoolean("isInScope", config.isInScope)
        persistenceData.setBoolean("proxy", config.proxy)
        persistenceData.setBoolean("repeater", config.repeater)
        persistenceData.setBoolean("nullCheck", config.nullCheck)

        // 保存其他数值类型配置
        persistenceData.setInteger("maxAllowedParameterCount", config.maxAllowedParameterCount)
        persistenceData.setLong("randomCheckTimer", config.randomCheckTimer)
        persistenceData.setLong("fixedIntervalTime", config.fixedIntervalTime)
        persistenceData.setString("neverScanRegex", config.neverScanRegex)

        // 保存 payloads
        var payloadsList = PersistedList.persistedStringList()
        payloadsList.clear()
        config.payloads.forEach { payload ->
            payloadsList.add(payload)
        }
        println(payloadsList.toString())
        persistenceData.setStringList("payloads", payloadsList)

        // 保存 heuristicWordsError
        var heuristicList = PersistedList.persistedStringList()
        heuristicList.clear()
        config.heuristicWordsError.forEach { word ->
            heuristicList.add(word)
        }
        persistenceData.setStringList("heuristicWordsError", heuristicList)

        // 保存 uninterestingType
        var uninterestingType = PersistedList.persistedStringList()
        uninterestingType.clear()
        config.uninterestingType.forEach { type ->
            uninterestingType.add(type)
        }
        persistenceData.setStringList("uninterestingType", uninterestingType)

        // 保存 allowedMimeTypeMimeType
        var mimeTypeList = PersistedList.persistedStringList()
        mimeTypeList.clear()
        config.allowedMimeTypeMimeType.forEach { mimeType ->
            mimeTypeList.add(mimeType)
        }
        persistenceData.setStringList("allowedMimeTypeMimeType", mimeTypeList)






//        persistenceData.setStringList("payloads", payloadsList)
//
//        var uninterestingType = PersistedList<String>.persistedStringList()
//        uninterestingType.clear()
//        config.uninterestingType.forEach { uninterestingType.add(it) }
//        persistenceData.setStringList("uninterestingType", uninterestingType)
//
//        var heuristicWordsError = PersistedList<String>.persistedStringList()
//        heuristicWordsError.clear()
//        config.heuristicWordsError.forEach { heuristicWordsError.add(it) }
//        persistenceData.setStringList("heuristicWordsError", heuristicWordsError)
//
//        var allowedMimeTypeMimeType = PersistedList<String>.persistedStringList()
//        allowedMimeTypeMimeType.clear()
//        config.allowedMimeTypeMimeType.forEach { allowedMimeTypeMimeType.add(it) }
//        persistenceData.setStringList("allowedMimeTypeMimeType", allowedMimeTypeMimeType)
    }

    /**
     * 更新配置并保存到持久化存储
     */
    fun updateConfig() {
        setData()
    }
}