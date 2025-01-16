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
        println("DataPersistence loadData ")
        // 加载基本配置
        config.startUP = persistenceData.getBoolean("startUP") ?: true
        config.isInScope = persistenceData.getBoolean("isInScope") ?: true
        config.proxy = persistenceData.getBoolean("proxy") ?: true
        config.repeater = persistenceData.getBoolean("repeater") ?: true
        config.nullCheck = persistenceData.getBoolean("nullCheck") ?: true

//         加载其他数值类型配置
        config.maxAllowedParameterCount = persistenceData.getInteger("maxAllowedParameterCount") ?: 30
        config.randomCheckTimer = persistenceData.getLong("randomCheckTimer") ?: 5000
        config.fixedIntervalTime = persistenceData.getLong("fixedIntervalTime") ?: 300
        config.neverScanRegex = persistenceData.getString("neverScanRegex") ?: "(delete|del)"

        // 加载列表类型配置
        persistenceData.getStringList("payloads")?.let {
            config.payloads.clear()
            for (i in it){
                config.payloads.add(i)
            }
        }

        persistenceData.getStringList("heuristicWordsError")?.let {
            config.heuristicWordsError.clear()
            for (i in it){
                config.heuristicWordsError.add(i)
            }
        }
        persistenceData.getStringList("uninterestingType")?.let {
            config.uninterestingType.clear()
            for (i in it){
                config.uninterestingType.add(i)
            }
        }
        persistenceData.getStringList("allowedMimeTypeMimeType")?.let {
            config.allowedMimeTypeMimeType.clear()
            for (i in it){
                config.allowedMimeTypeMimeType.add(i)
            }
        }
    }

    private fun setData() {
        // 保存基本配置
        persistenceData.setBoolean("startUP", config.startUP)
        persistenceData.setBoolean("isInScope", config.isInScope)
        persistenceData.setBoolean("proxy", config.proxy)
        persistenceData.setBoolean("repeater", config.repeater)
        persistenceData.setBoolean("nullCheck", config.nullCheck)

//        // 保存其他数值类型配置
        persistenceData.setInteger("maxAllowedParameterCount", config.maxAllowedParameterCount)
        persistenceData.setLong("randomCheckTimer", config.randomCheckTimer)
        persistenceData.setLong("fixedIntervalTime", config.fixedIntervalTime)
        persistenceData.setString("neverScanRegex", config.neverScanRegex)


//         保存 payloads
        val payloadsList = PersistedList<String>.persistedStringList()
        payloadsList.clear()
        payloadsList.addAll(config.payloads)
        persistenceData.setStringList("payloads", payloadsList)



//         保存 heuristicWordsError
        val heuristicList = PersistedList<String>.persistedStringList()
        heuristicList.clear()
        heuristicList.addAll(config.heuristicWordsError)
        persistenceData.setStringList("heuristicWordsError", heuristicList)

////         保存 uninterestingType
        val uninterestingType = PersistedList<String>.persistedStringList()
        uninterestingType.clear()
        uninterestingType.addAll(config.uninterestingType)
        persistenceData.setStringList("uninterestingType", uninterestingType)

//         保存 allowedMimeTypeMimeType
        val mimeTypeList = PersistedList<String>.persistedStringList()
        mimeTypeList.clear()
        mimeTypeList.addAll(config.allowedMimeTypeMimeType)
        persistenceData.setStringList("allowedMimeTypeMimeType", mimeTypeList)


    }

    /**
     * 更新配置并保存到持久化存储
     */
    fun updateConfig() {
        setData()
    }
}