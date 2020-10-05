package org.jetbrains.jps.build.snapshot.dumper.actions

import com.intellij.compiler.server.BuildManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.cmdline.LogSetup
import java.io.*
import java.util.*

class EnableLoggingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val logDirectory = BuildManager.getBuildLogDirectory()
        val buildLogProperties = File(logDirectory, LogSetup.LOG_CONFIG_FILE_NAME)
        val config: InputStream
        if(buildLogProperties.exists()) {
            config = FileInputStream(buildLogProperties)
        } else {
            config = LogSetup.readDefaultLogConfig()
        }

        val properties = Properties().also {
            it.load(config)
            it.setProperty("log4j.logger.org.jetbrains.jps", "debug")
            it.setProperty("log4j.logger.#org.jetbrains.jps", "debug")
        }

        FileUtil.delete(logDirectory)
        FileUtil.createDirectory(logDirectory)

        BufferedOutputStream(FileOutputStream(buildLogProperties)).use { output -> properties.store(output, null) }
    }
}