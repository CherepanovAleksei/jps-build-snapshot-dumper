package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.compiler.server.BuildManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.util.*

class Collector(private val project: Project) {
    private val PROJECT_DIRS_TO_COLLECT = listOf(".idea", "out", "dist", "buildSrc/build/classes/java")
    private val LOG = Logger.getInstance("org.jetbrains.jps.build.snapshot.dumper.Collector")
    private val projectPath = project.basePath

    fun collect(){
        val tempDir: File = FileUtil.createTempDirectory("jps_build_snapshot", ".tmp", true)
        try {
            copyProjectDirsTo(tempDir)
            copyCachesTo(tempDir)
            copyLogsTo(tempDir)
            //TODO: add info (about)
            val zipFile = File(projectPath, "jps_build_snapshot_" + UUID.randomUUID().toString() + ".zip")
            Zipper.zip(zipFile, tempDir)

            BalloonNotification(project).showBalloon(zipFile.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun copyProjectDirsTo(tempDir: File) {

        for (dir in PROJECT_DIRS_TO_COLLECT) {
            val dirToCopy = File(projectPath, dir)
            if(!dirToCopy.exists()) {
                LOG.debug("$dir dir does not exists: skip it")
                continue
            }
            val newDir = File(tempDir, dir)
            FileUtil.copyDir(dirToCopy, newDir)
            LOG.debug("$dir directory is added")
        }
    }

    private fun copyCachesTo(tempDir: File) {
        val asd = BuildManager.getInstance()
        val caches = File(BuildManager.getInstance().getProjectSystemDirectory(project)?.absolutePath!!)
        if(!caches.exists()) {
            LOG.debug("Caches dir does not exists: skip it")
            return
        }
        val newDir = File(tempDir, "compile-server")
        FileUtil.copyDir(caches, newDir)
        LOG.debug("Caches directory is added")
    }

    private fun copyLogsTo(tempDir: File) {
        val logDirectory = BuildManager.getBuildLogDirectory()
        if(!logDirectory.exists()) {
            LOG.debug("Logs dir does not exists: skip it")
            return
        }
        val newDir = File(tempDir, "logs")
        FileUtil.copyDir(logDirectory, newDir)
        LOG.debug("Logs directory is added")
    }
}