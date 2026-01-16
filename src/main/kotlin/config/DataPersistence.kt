package config

import burp.api.montoya.MontoyaApi
import burp.api.montoya.persistence.PersistedList
import burp.api.montoya.persistence.PersistedObject

class DataPersistence(val api: MontoyaApi) {
    private val persistenceData: PersistedObject = api.persistence().extensionData() ?: throw IllegalStateException("Persistence not available")
    val config = Configs.INSTANCE

    private companion object {
        const val KEY_FLAG = "JAVELEYFLAG"
    }

    init {
        if (persistenceData.getString(KEY_FLAG) == null) {
            persistenceData.setString(KEY_FLAG, KEY_FLAG)
            saveData()
        } else {
            loadData()
        }
    }

    fun loadData() {
        with(persistenceData) {
            getBoolean("startUP")?.let { config.startUP = it }
            getBoolean("isInScope")?.let { config.isInScope = it }
            getBoolean("proxy")?.let { config.proxy = it }
            getBoolean("repeater")?.let { config.repeater = it }
            getBoolean("nullCheck")?.let { config.nullCheck = it }

            getInteger("maxAllowedParameterCount")?.let { config.maxAllowedParameterCount = it }
            getLong("randomCheckTimer")?.let { config.randomCheckTimer = it }
            getLong("fixedIntervalTime")?.let { config.fixedIntervalTime = it }
            getString("neverScanRegex")?.let { config.neverScanRegex = it }
            getString("nestedJsonParams")?.let { config.nestedJsonParams = it }

            // 加载列表：PersistedList
            loadListToConfig("payloads", config.payloads)
            loadListToConfig("boringWords", config.boringWords)
            loadListToConfig("uninterestingType", config.uninterestingType)
            loadListToConfig("allowedMimeTypeMimeType", config.allowedMimeTypeMimeType)
            loadListToConfig("hiddenParams", config.hiddenParams)
            loadListToConfig("ignoreParams", config.ignoreParams)
        }
    }

    fun saveData() {
        with(persistenceData) {
            setBoolean("startUP", config.startUP)
            setBoolean("isInScope", config.isInScope)
            setBoolean("proxy", config.proxy)
            setBoolean("repeater", config.repeater)
            setBoolean("nullCheck", config.nullCheck)

            setInteger("maxAllowedParameterCount", config.maxAllowedParameterCount)
            setLong("randomCheckTimer", config.randomCheckTimer)
            setLong("fixedIntervalTime", config.fixedIntervalTime)
            setString("neverScanRegex", config.neverScanRegex)
            setString("nestedJsonParams", config.nestedJsonParams)

            // 修复点：调用 .toPersistedList() 进行类型转换
            setStringList("payloads", config.payloads.toPersistedList())
            setStringList("hiddenParams", config.hiddenParams.toPersistedList())
            setStringList("boringWords", config.boringWords.toPersistedList())
            setStringList("ignoreParams", config.ignoreParams.toPersistedList())
            setStringList("uninterestingType", config.uninterestingType.toPersistedList())
            setStringList("allowedMimeTypeMimeType", config.allowedMimeTypeMimeType.toPersistedList())
        }
    }

    fun updateConfig() = saveData()

    private fun PersistedObject.loadListToConfig(key: String, targetList: MutableList<String>) {
        this.getStringList(key)?.let { savedList ->
            targetList.clear()
            savedList.forEach {
                targetList.add(it.toString())
            }
        }
    }


    private fun List<String>.toPersistedList(): PersistedList<String> {
        val list = PersistedList.persistedStringList()
        // 将当前 List 的所有元素添加进去
        list.addAll(this)
        return list
    }
}