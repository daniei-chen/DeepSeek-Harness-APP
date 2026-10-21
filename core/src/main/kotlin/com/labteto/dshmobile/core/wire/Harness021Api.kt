package com.labteto.dshmobile.core.wire

import com.labteto.dshmobile.core.wire.dto.*
import kotlinx.serialization.serializer
import kotlinx.serialization.json.*

private suspend inline fun <reified T> DshApiClient.featureCall(endpoint: String, args: JsonObject = JsonObject(emptyMap())): RpcResult<T> =
    call(endpoint, args, serializer<T>())

private fun named(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
private fun str(value: String) = JsonPrimitive(value)
private fun request(value: JsonElement) = named("request" to value)

fun AutomationTask.expectedRecord(): JsonObject = JsonObject(
    WireJson.encodeToJsonElement(AutomationTask.serializer(), this).jsonObject.filterKeys {
        it !in setOf("sessionId", "status", "lastDelivery")
    },
)

suspend fun DshApiClient.automationCatalog(): RpcResult<List<AutomationTask>> = featureCall("schedule/catalog")
suspend fun DshApiClient.automationList(sessionId: String): RpcResult<List<AutomationTask>> =
    featureCall("schedule/list", request(named("sessionId" to str(sessionId))))
suspend fun DshApiClient.automationHistory(sessionId: String, id: String, before: String? = null): RpcResult<AutomationHistory> =
    featureCall("schedule/history", request(buildJsonObject {
        put("sessionId", sessionId); put("id", id); put("limit", 25)
        before?.let { put("before", it) }
    }))
suspend fun DshApiClient.automationUpdate(value: AutomationUpdate): RpcResult<AutomationMutation> =
    featureCall("schedule/update", request(WireJson.encodeToJsonElement(AutomationUpdate.serializer(), value)))
suspend fun DshApiClient.automationDelete(sessionId: String, id: String): RpcResult<AutomationMutation> =
    featureCall("schedule/delete", request(named("sessionId" to str(sessionId), "id" to str(id))))

suspend fun DshApiClient.answerContinuedQuestion(sessionId: String, callId: String, answer: AskUserQuestionAnswer): RpcResult<Boolean> =
    featureCall("userQuestions/answer", named("agentId" to str(sessionId), "callId" to str(callId),
        "answer" to WireJson.encodeToJsonElement(AskUserQuestionAnswer.serializer(), answer)))

suspend fun DshApiClient.pluginBundles(): RpcResult<List<PluginBundle>> = featureCall("pluginManager/listBundles")
suspend fun DshApiClient.managedPlugins(): RpcResult<List<ManagedPlugin>> = featureCall("pluginManager/listPlugins")
suspend fun DshApiClient.pluginRegistries(): RpcResult<PluginRegistries> = featureCall("pluginManager/registries")
suspend fun DshApiClient.inspectPlugin(spec: String, registry: String? = null): RpcResult<PluginInspection> =
    featureCall("pluginManager/inspect", buildJsonObject {
        put("spec", spec)
        registry?.let { put("options", named("registry" to str(it))) }
    })
suspend fun DshApiClient.installPlugin(spec: String, options: PluginInstallOptions): RpcResult<PluginChangeResult> =
    featureCall("pluginManager/installBundle", named("spec" to str(spec), "options" to WireJson.encodeToJsonElement(PluginInstallOptions.serializer(), options)))
suspend fun DshApiClient.waitForPluginInstall(requestId: String): RpcResult<PluginChangeResult?> =
    featureCall("pluginManager/waitForInstall", named("requestId" to str(requestId)))
suspend fun DshApiClient.cancelPluginInstall(requestId: String): RpcResult<PluginInstallCancellation> =
    featureCall("pluginManager/cancelInstall", named("requestId" to str(requestId)))
suspend fun DshApiClient.setBundleEnabled(name: String, enabled: Boolean): RpcResult<PluginChangeResult> =
    featureCall("pluginManager/setBundleEnabled", named("name" to str(name), "enabled" to JsonPrimitive(enabled)))
suspend fun DshApiClient.setPluginEnabled(id: String, enabled: Boolean): RpcResult<PluginChangeResult> =
    featureCall("pluginManager/setPluginEnabled", named("id" to str(id), "enabled" to JsonPrimitive(enabled)))
suspend fun DshApiClient.removePluginBundle(name: String): RpcResult<PluginChangeResult> =
    featureCall("pluginManager/removeBundle", named("name" to str(name)))
suspend fun DshApiClient.sessionReferenceCandidates(sessionId: String, query: String): RpcResult<List<ReferenceCandidate>> =
    featureCall("sessionReferenceResolver/candidates", named("agentId" to str(sessionId), "query" to str(query)))

class HarnessFeatureException(val code: String, message: String) : Exception(message)
fun <T> RpcResult<T>.featureValue(): T = when (this) {
    is RpcResult.Ok -> value
    is RpcResult.Err -> throw HarnessFeatureException(error.code, error.message)
}
