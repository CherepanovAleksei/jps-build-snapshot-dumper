package org.jetbrains.jps.build.snapshot.dumper.actions

import com.intellij.compiler.server.BuildManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.cmdline.LogSetup
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class EnableLoggingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val logDirectory = BuildManager.getBuildLogDirectory()
        FileUtil.delete(logDirectory)
        FileUtil.createDirectory(logDirectory)
        val properties = Properties()
        LogSetup.readDefaultLogConfig().use { config -> properties.load(config) }
        properties.setProperty("log4j.rootLogger", "debug, file")
        val logFile = File(logDirectory, LogSetup.LOG_CONFIG_FILE_NAME)
        BufferedOutputStream(FileOutputStream(logFile)).use { output -> properties.store(output, null) }
    }
}