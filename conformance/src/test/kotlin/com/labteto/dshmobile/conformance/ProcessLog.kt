package com.labteto.dshmobile.conformance

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Full merged stdout/stderr, retained after the disposable process and workspace are gone. */
internal class ProcessLog(kind: String) {
    private val owner = Throwable().stackTrace.firstOrNull {
        it.className.startsWith("com.labteto.dshmobile.conformance.") &&
            it.className.substringBefore('$').endsWith("Test")
    }?.className?.substringBefore('$')?.substringAfterLast('.') ?: "Conformance"
    private val number = counters.computeIfAbsent("$owner-$kind") { AtomicInteger() }.incrementAndGet()
    private val file = File("build/conformance-logs/$owner-$number-$kind.log")
    private val transcript = StringBuilder()

    @Synchronized fun append(line: String) {
        transcript.appendLine(line.replace(Regex("token=[A-Za-z0-9_-]+"), "token=[redacted]"))
    }

    @Synchronized fun text(): String = transcript.toString()

    @Synchronized fun write() {
        file.parentFile.mkdirs()
        file.writeText(transcript.toString())
    }

    private companion object {
        val counters = ConcurrentHashMap<String, AtomicInteger>()
    }
}
