package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.compiler.server.BuildManager
import com.intellij.ide.AboutPopupDescriptionProvider
import com.intellij.ide.IdeBundle
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.LicensingFacade
import com.intellij.util.ObjectUtils
import com.intellij.util.text.DateFormatUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import java.io.File
import java.io.IOException
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.util.*
import git4idea.commands.GitLineHandler
import java.util.stream.Collectors


class Collector(private val project: Project, private val additionalFoldersToCollect: String = "") {
    private val PROJECT_DIRS_TO_COLLECT = listOf(".idea", "out", "dist", "buildSrc/build/classes/java")
    private val LOG = Logger.getInstance("org.jetbrains.jps.build.snapshot.dumper.Collector")
    private val projectPath = project.basePath
    private val balloonNotification = BalloonNotification()

    fun collectInBackground() {
        val task = object : Task.Backgroundable(project, "Collecting info about JPS build") {
            override fun run(indicator: ProgressIndicator) {
                collect()
            }
        }

        val processIndicator = BackgroundableProcessIndicator(task)
        processIndicator.isIndeterminate = true
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, processIndicator)
    }

    fun collect() {
        val tempDir: File = FileUtil.createTempDirectory("jps_build_snapshot", ".tmp", true)
        var isFailed = false

        try {
            copyProjectDirsTo(tempDir)
            copyCachesTo(tempDir)
            copyLogsTo(tempDir)
            addAboutInfo(tempDir)
            addGitInfo(tempDir)
            copyKotlinDaemonLogs(tempDir)

            val zipFile = File(projectPath, "jps_build_snapshot_" + UUID.randomUUID().toString() + ".zip")
            Zipper().zip(zipFile, tempDir)

            balloonNotification.showSuccessBalloon(zipFile.absolutePath)
        } catch (e: Exception) {
            LOG.error(e)
            isFailed = true
        } finally {
            tempDir.deleteRecursively()
        }

        if(isFailed) balloonNotification.showCollectFailBalloon(this)
    }

    private fun copyKotlinDaemonLogs(tempDir: File) {
        val tempSystemFolder = File(System.getenv("TMPDIR"))
        if(tempSystemFolder.exists()) {
            val logs = tempSystemFolder.listFiles()?.filter{it.name.matches(Regex("kotlin-daemon.*.log")) }
            if(logs == null) {
                LOG.debug("There are no logs for Kotlin Daemon on your system. Skip Kotlin daemon logs collecting")
                return
            }

            val kotlinDaemonLastLog = logs.sorted().last()

            val newDir = File(tempDir, "kotlinDaemonLogs")
            copyFileOrDir("KotlinDaemonLogs", kotlinDaemonLastLog, newDir)
        } else {
            LOG.debug("TMPDIR is not set on your system. Skip Kotlin daemon logs collecting")
        }
    }

    private fun copyProjectDirsTo(tempDir: File) {
        if(additionalFoldersToCollect.isBlank()) {
            LOG.debug("There are no additional paths. Skip it.")
            return
        }

        val additionalFolders = additionalFoldersToCollect.split(",")
            .stream()
            .map { it.trim() }
            .collect(Collectors.toList())

        for (dirName in additionalFolders + PROJECT_DIRS_TO_COLLECT) {
            val dirToCopy = File(projectPath, dirName)
            val newDir = File(tempDir, dirName)
            copyFileOrDir(dirName, dirToCopy, newDir)
        }
    }

    private fun copyCachesTo(tempDir: File) {
        val caches = File(BuildManager.getInstance().getProjectSystemDirectory(project)?.absolutePath!!)
        val newDir = File(tempDir, "compile-server")
        copyFileOrDir("Caches", caches, newDir)
    }

    private fun copyLogsTo(tempDir: File) {
        val logDirectory = BuildManager.getBuildLogDirectory()
        val newDir = File(tempDir, "logs")
        copyFileOrDir("Logs", logDirectory, newDir)
    }

    private fun copyFileOrDir(dirName: String, fromDir: File, toDir: File){
        if(!fromDir.exists()) {
            LOG.debug("${fromDir.name} dir does not exists: skip it")
            return
        }

        FileUtil.copyFileOrDir(fromDir, toDir)
        LOG.debug("$dirName directory is added")
    }

    private fun addGitInfo(tempDir: File) {
        addChangesPatch(tempDir)
        addGitStatusInfo(tempDir)
        addCommitInfo(tempDir)
    }

    private fun addChangesPatch(tempDir: File) {
        val patchFile = File(tempDir, "git-changes-patch.txt")

        val diff = GitLineHandler(project, File(projectPath!!), GitCommand.DIFF)
        diff.addParameters("HEAD")
        diff.setStdoutSuppressed(true)
        diff.setStderrSuppressed(true)
        diff.setSilent(true)

        val content: String = Git.getInstance().runCommand(diff).getOutputOrThrow()

        LOG.debug(content)
        patchFile.writeText(content)
        LOG.debug("AboutInfo file is added")
    }

    private fun addCommitInfo(tempDir: File) {
        val gitInfoFile = File(tempDir, "git-commit-info.txt")

        val commit = GitLineHandler(project, File(projectPath!!), GitCommand.LOG)
        commit.addParameters("-n 1")
        val content: String = Git.getInstance().runCommand(commit).getOutputOrThrow()

        LOG.debug(content)
        gitInfoFile.writeText(content)
        LOG.debug("Git commit info file is added")
    }

    private fun addGitStatusInfo(tempDir: File) {
        val gitInfoFile = File(tempDir, "git-status-info.txt")

        val status = GitLineHandler(project, File(projectPath!!), GitCommand.STATUS)
        val content: String = Git.getInstance().runCommand(status).getOutputOrThrow()

        LOG.debug(content)
        gitInfoFile.writeText(content)
        LOG.debug("Git branch info file is added")
    }

    private fun addAboutInfo(tempDir: File) {
        val aboutFile = File(tempDir, "about.txt")
        // Just copied from com/intellij/ide/actions/AboutPopup.java
        val content = getAboutInfo() + getExtraInfo()
        LOG.debug(content)
        aboutFile.writeText(content)
        LOG.debug("AboutInfo file is added")
    }

    private fun getAboutInfo(): String {
        var aboutInfo = ""

        val appInfo = ApplicationInfoEx.getInstanceEx() as ApplicationInfoImpl

        var appName = appInfo.fullApplicationName
        val edition = ApplicationNamesInfo.getInstance().editionName
        if (edition != null) appName += " ($edition)"

        aboutInfo += "$appName\n"

        var buildInfo = IdeBundle.message("about.box.build.number", appInfo.build.asString())
        val cal = appInfo.buildDate
        var buildDate: String? = ""
        if (appInfo.build.isSnapshot) {
            buildDate = SimpleDateFormat("HH:mm, ").format(cal.time)
        }
        buildDate += DateFormatUtil.formatAboutDialogDate(cal.time)
        buildInfo += IdeBundle.message("about.box.build.date", buildDate!!)
        aboutInfo += "$buildInfo\n"


        val la = LicensingFacade.getInstance()
        if (la != null) {
            val licensedTo = la.licensedToMessage
            if (licensedTo != null) {
                aboutInfo += "$licensedTo\n"
            }
            for (message in la.licenseRestrictionsMessages) {
                aboutInfo += "$message\n"
            }
        }


        val properties = System.getProperties()
        val javaVersion = properties.getProperty("java.runtime.version", properties.getProperty("java.version", "unknown"))
        val arch = properties.getProperty("os.arch", "")
        aboutInfo += "${IdeBundle.message("about.box.jre", javaVersion, arch)}\n"


        val vmVersion = properties.getProperty("java.vm.name", "unknown")
        val vmVendor = properties.getProperty("java.vendor", "unknown")
        aboutInfo += "${IdeBundle.message("about.box.vm", vmVersion, vmVendor)}\n"

        val EP_NAME = ExtensionPointName<AboutPopupDescriptionProvider>("com.intellij.aboutPopupDescriptionProvider")

        for (aboutInfoProvider in EP_NAME.extensions) {
            val description = aboutInfoProvider.getDescription() ?: continue
            val lines = description.split("[\n]+".toRegex()).toTypedArray()
            if (lines.size == 0) continue
        }

        aboutInfo += "${IdeBundle.message("about.box.powered.by") + " "}\n"

        val thirdPartyLibraries = loadThirdPartyLibraries()
        if (thirdPartyLibraries != null) {
            aboutInfo += "${IdeBundle.message("about.box.open.source.software")}\n"
        } else {
            // When compiled from sources, third-party-libraries.html file isn't generated, so window can't be shown
            aboutInfo += "${IdeBundle.message("about.box.open.source.software")}\n"
        }
        return aboutInfo
    }

    private fun loadThirdPartyLibraries(): String? {
        val thirdPartyLibrariesFile = File(PathManager.getHomePath(), "license/third-party-libraries.html")
        if (thirdPartyLibrariesFile.isFile) {
            try {
                return FileUtil.loadFile(thirdPartyLibrariesFile)
            } catch (e: IOException) {
                LOG.warn(e)
            }
        }
        return null
    }

    private fun getExtraInfo(): String {
        var extraInfo = SystemInfo.getOsNameAndVersion()
        extraInfo += "GC: " + ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map { obj: GarbageCollectorMXBean -> obj.name }.collect(StringUtil.joining()) + "\n"
        extraInfo += "Memory: ${Runtime.getRuntime().maxMemory() / FileUtilRt.MEGABYTE}M\n"
        extraInfo += "Cores: ${Runtime.getRuntime().availableProcessors()}\n"
        val registryKeys = Registry.getAll().stream().filter { obj: RegistryValue -> obj.isChangedFromDefault }
                .map { v: RegistryValue -> v.key + "=" + v.asString() }.collect(StringUtil.joining())
        if (!StringUtil.isEmpty(registryKeys)) {
            extraInfo += "Registry: $registryKeys\n"
        }
        val nonBundledPlugins = Arrays.stream(PluginManagerCore.getPlugins())
                .filter { p: IdeaPluginDescriptor -> !p.isBundled && p.isEnabled }
                .map { p: IdeaPluginDescriptor -> p.pluginId.idString }
                .collect(StringUtil.joining())
        if (!StringUtil.isEmpty(nonBundledPlugins)) {
            extraInfo += "Non-Bundled Plugins: $nonBundledPlugins"
        }
        if (SystemInfo.isUnix && !SystemInfo.isMac) {
            extraInfo += "\nCurrent Desktop: ${ObjectUtils.notNull(System.getenv("XDG_CURRENT_DESKTOP"), "Undefined")}"
        }
        return extraInfo
    }
}