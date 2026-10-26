package com.twilight.calleditor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

data class PhoneAccountChoice(val id: String, val component: String, val label: String, val slot: Int? = null)
data class PhoneAccountState(
    val accounts: List<PhoneAccountChoice> = emptyList(),
    val permissionGranted: Boolean = false,
    val message: String? = null,
)

/** Blocking system-service calls; run on Dispatchers.IO. Never infer a slot from an account ID. */
class PhoneAccounts(context: Context) {
    private val context = context.applicationContext

    fun load(): PhoneAccountState {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return PhoneAccountState(message = "允许读取电话状态后可选择 SIM；不授权仍可保留原账户并编辑其他内容。")
        }
        return try {
            val telecom = context.getSystemService(TelecomManager::class.java)
                ?: return PhoneAccountState(permissionGranted = true, message = "设备不提供电话账户服务。")
            val telephony = context.getSystemService(TelephonyManager::class.java)
            val subscriptions = runCatching {
                context.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList.orEmpty()
            }.getOrDefault(emptyList())
            val choices = telecom.callCapablePhoneAccounts.mapIndexed { index, handle ->
                val info = if (Build.VERSION.SDK_INT >= 30 && telephony != null) {
                    runCatching { telephony.getSubscriptionId(handle) }.getOrNull()?.let { id ->
                        subscriptions.firstOrNull { it.subscriptionId == id }
                    }
                } else null
                val slot = info?.simSlotIndex?.takeIf { it >= 0 }
                val systemLabel = runCatching { telecom.getPhoneAccount(handle)?.label?.toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "电话账户 ${index + 1}"
                PhoneAccountChoice(handle.id, handle.componentName.flattenToString(),
                    if (slot != null) "SIM ${slot + 1} · $systemLabel" else systemLabel, slot)
            }.distinctBy { it.id to it.component }.sortedWith(compareBy({ it.slot ?: Int.MAX_VALUE }, { it.label }))
            PhoneAccountState(choices, true, if (choices.isEmpty()) "未找到可用电话账户，请检查 SIM 是否启用后刷新。" else null)
        } catch (_: SecurityException) {
            PhoneAccountState(message = "无法读取电话账户，请允许电话状态权限或在应用设置中检查授权。")
        } catch (_: Exception) {
            PhoneAccountState(permissionGranted = true, message = "电话账户读取失败，请稍后刷新；原账户仍会保留。")
        }
    }

    fun requireAvailable(entry: CallEntry) {
        val state = load()
        require(state.accounts.any { it.id == entry.accountId && it.component == entry.accountComponent }) {
            "所选电话账户已不可用或权限已撤销，请刷新账户列表并重新选择。"
        }
    }
}
