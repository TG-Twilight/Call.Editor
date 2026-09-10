package com.twilight.calleditor

// This device scope applies only to Breeno Provider metadata, not known type names.
internal fun supportsVendorCallStatus(model: String, sdk: Int) = model == "RMX5200" && sdk == 36

internal val observedVendorCallTypes = listOf(-1, -3, -5, 27, 51, 52)
internal const val FILTER_OTHER = -100
internal const val FILTER_BLACKLIST = -101
internal const val FILTER_BREENO = -102
private const val BREENO_CALL_FEATURE = 0x20000000

/** Live Provider metadata only; version 1 backups do not contain these fields or linked content. */
data class VendorCallDetails(val features: Int?, val virtualCallId: String?)

// Known raw types use the names established by the Contacts/Provider investigation.
fun typeLabel(type: Int): String = when (type) {
    1 -> "呼入"
    2 -> "呼出"
    3 -> "未接"
    4 -> "语音信箱"
    5 -> "已拒接"
    6 -> "已拦截"
    7 -> "其他设备接听"
    -1 -> "呼入（黑名单归类）"
    -3 -> "未接（黑名单归类）"
    -5 -> "已拒接（黑名单归类）"
    27 -> "黑名单号码（拦截分类）"
    51 -> "骚扰电话（拦截分类）"
    52 -> "广告推销（拦截分类）"
    else -> "其他（$type）"
}

internal fun editableCallTypes(originalType: Int): List<Int> =
    ((1..7).toList() + observedVendorCallTypes + originalType).distinct()

internal fun CallEntry.isBreenoCall(vendorSupported: Boolean): Boolean = vendorSupported &&
    ((vendorDetails?.features ?: 0) and BREENO_CALL_FEATURE != 0) && !vendorDetails?.virtualCallId.isNullOrEmpty()

internal fun CallEntry.statusLabel(vendorSupported: Boolean): String =
    typeLabel(type) + if (isBreenoCall(vendorSupported)) " · 小布代接" else ""

internal fun CallEntry.baseCallType(): Int = when (type) {
    -1 -> 1
    -3 -> 3
    -5 -> 5
    27, 51, 52 -> 6
    else -> type
}

internal fun CallEntry.matchesCallFilter(filter: Int, vendorSupported: Boolean): Boolean = when (filter) {
    0 -> true
    FILTER_OTHER -> type !in 1..7 && type !in observedVendorCallTypes
    FILTER_BLACKLIST -> type in listOf(-1, -3, -5, 27)
    FILTER_BREENO -> isBreenoCall(vendorSupported)
    else -> type == filter || baseCallType() == filter
}

/** Changes only the answering bit; no association ID or other feature is invented or cleared. */
internal fun editedBreenoFeatures(details: VendorCallDetails?, enabled: Boolean): Int {
    val features = requireNotNull(details?.features) { "无法读取小布标记，请刷新记录后重试" }
    require(!details.virtualCallId.isNullOrEmpty()) { "此记录没有小布关联内容编号，无法设置代接标记" }
    return if (enabled) features or BREENO_CALL_FEATURE else features and BREENO_CALL_FEATURE.inv()
}

internal data class BreenoEdit(val features: Int, val expectedFeatures: Int, val virtualCallId: String)

internal fun planBreenoEdit(entry: CallEntry, current: CallEntry?, enabled: Boolean?, vendorSupported: Boolean): BreenoEdit? {
    if (enabled == null) return null
    require(vendorSupported && entry.id != 0L && current?.id == entry.id) { "此设备或记录不支持编辑小布代接标记" }
    require(current.vendorDetails == entry.vendorDetails) { "小布关联信息已变化，请重新打开记录后编辑" }
    val features = editedBreenoFeatures(current.vendorDetails, enabled)
    return BreenoEdit(features, current.vendorDetails!!.features!!, current.vendorDetails.virtualCallId!!)
}
