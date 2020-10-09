package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File

class BalloonNotification {
    private val displayId = "KotlinJpsBuildSnapshotDumper"
    private val notificationGroup = NotificationGroup(displayId, NotificationDisplayType.BALLOON, true)
    private var notification: Notification? = null

    fun showCollectBalloon(project: Project) {
        val title = "Collect JPS build info"
        val content = "This action will collect all JPS build artifacts: " +
                "all build output (like out, dist, buildSrc folders), " +
                "logs and build caches."
        val action = object : AnAction("Collect", "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                notification?.expire()
                Collector(project, this@BalloonNotification).collectInBackground()
            }
        }

        showBalloon(title, content, action)
    }

    fun showSuccessBalloon(pathToArchive: String) {
        val title = "JPS Build Snapshot successfully dumped"
        val content = "Zip archive: $pathToArchive"
        val action = object : AnAction("Delete archive", "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                notification?.expire()
                val archive = File(pathToArchive)
                if(archive.exists()) archive.delete()
            }
        }

        showBalloon(title, content, action)
    }

    fun showNoProjectBalloon() {
        val title = "There is no open project"
        val content = "To use these feature, you should open project and build it via JPS"
        showBalloon(title, content)
    }

    fun showCollectFailBalloon(collector: Collector) {
        val title = "JPS build info collection failed"
        val content = "Error is logged"
        val action = object : AnAction("Retry", "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                notification?.expire()
                collector.collect()
            }
        }

        showBalloon(title, content, action)
    }

    private fun showBalloon(title: String, content: String = "", action: AnAction? = null) {
        notification = notificationGroup.createNotification(title, content, NotificationType.INFORMATION)
        if(action!=null){
            notification!!.addAction(action)
        }
        Notifications.Bus.notify(notification!!)
    }
}