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
import java.io.File
import java.io.IOException
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.util.*

class Collector(private val project: Project) {
    private val PROJECT_DIRS_TO_COLLECT = listOf(".idea", "out", "dist", "buildSrc/build/classes/java")
    private val LOG = Logger.getInstance("org.jetbrains.jps.build.snapshot.dumper.Collector")
    private val projectPath = project.basePath

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

    private fun collect() {
        val tempDir: File = FileUtil.createTempDirectory("jps_build_snapshot", ".tmp", true)
        try {
            copyProjectDirsTo(tempDir)
            copyCachesTo(tempDir)
            copyLogsTo(tempDir)
            addAboutInfo(tempDir)
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

    private fun addAboutInfo(tempDir: File) {
        val aboutFile = File(tempDir, "about.txt")
        //just copied from com/intellij/ide/actions/AboutPopup.java
        val content = getAboutInfo() + getExtraInfo()
        LOG.debug(content)
        aboutFile.writeText(content)
        LOG.debug("AboutInfo file is added")
    }

    private fun getAboutInfo(): String? {
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