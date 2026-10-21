package com.labteto.dshmobile.ui.screens.main

import com.labteto.dshmobile.data.PluginOperationState
import com.labteto.dshmobile.core.wire.dto.PluginChangeResult
import org.junit.Assert.*
import org.junit.Test

class PluginPresentationTest {
    @Test fun `catalog updates retain disabled selection and use the exact host target`() {
        val bundle = com.labteto.dshmobile.core.wire.dto.PluginBundle("codex", enabled = false, installed = true,
            availability = "profile", installTarget = com.labteto.dshmobile.core.wire.dto.PluginInstallTarget("codex@0.2.1-alpha.2", "0.2.1-alpha.2"))
        val update = bundle.catalogInstall(bundle.enabled)!!
        assertEquals("codex@0.2.1-alpha.2", update.target)
        assertFalse(update.enabled)
        assertEquals(true, update.saveExact)
        val enable = bundle.catalogInstall(true)!!
        val approval = PluginOperationState(subject = enable.target, enabled = enable.enabled, saveExact = enable.saveExact,
            result = PluginChangeResult(false, "failed", "install", "codex", pendingBuilds = listOf("addon@1"))).approvalSubject()!!
        assertTrue(approval.enabled)
        assertEquals(true, approval.saveExact)
        assertEquals(listOf("addon@1"), approval.approvedBuilds)
        assertNull(bundle.copy(installTarget = com.labteto.dshmobile.core.wire.dto.PluginInstallTarget("link:/source", "dev")).catalogInstall(true)?.saveExact)
    }
    @Test fun `approval captures operation package registry and scripts independently of form`() {
        val state = PluginOperationState(host = "host", requestId = "request", subject = "@scope/first@1", registry = "https://registry.example",
            result = PluginChangeResult(false, "failed", "install", "@scope/first", pendingBuilds = listOf("native-addon@2")))
        val approval = state.approvalSubject()!!
        val editedForm = PluginInstallSubject("other@9", "https://other.example")
        assertNotEquals(editedForm.target, approval.target)
        assertEquals("@scope/first@1", approval.target)
        assertEquals("https://registry.example", approval.registry)
        assertEquals(listOf("native-addon@2"), approval.approvedBuilds)
        assertEquals("@scope/first@1", state.copy(busy = false, unknown = true).displaySubject())
    }
    @Test fun `legacy unknown operation never exposes UUID as package or invents approval target`() {
        val state = PluginOperationState(host = "host", requestId = "opaque-id", unknown = true)
        assertNull(state.displaySubject())
        assertNull(state.approvalSubject())
    }
    @Test fun `successful noninstall mutation identifies its returned package`() {
        val state = PluginOperationState(result = PluginChangeResult(true, "applied", "enable", "package"))
        assertEquals("package", state.displaySubject())
        assertNull(state.approvalSubject())
    }
}
