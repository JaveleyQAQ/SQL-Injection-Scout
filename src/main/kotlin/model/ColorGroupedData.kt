package model

import model.logentry.ModifiedLogDataModel

data class ColorGroupedData(
    val parameter: String,
    val entries: List<ModifiedLogDataModel>,
    val colorPriority: Int
)