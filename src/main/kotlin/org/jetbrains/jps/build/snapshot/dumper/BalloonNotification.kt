package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.io.File

class BalloonNotification {
    private val displayId = "KotlinJpsBuildSnapshotDumper"
    private val notificationGroup = NotificationGroup(displayId, NotificationDisplayType.BALLOON, true)
    private var notification: Notification? = null

    fun showSuccessBalloon(pathToArchive: String) {
        val title = "JPS Build Snapshot successfully dumped"
        val content = "Zip archive: $pathToArchive"

        val revealAction = object : AnAction(RevealFileAction.getActionName(), "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                val archive = File(pathToArchive)
                if (archive.exists()) {
                    notification?.hideBalloon()
                    RevealFileAction.openFile(archive)
                }
            }
        }

        val deleteAction = object : AnAction("Delete archive", "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                notification?.expire()
                val archive = File(pathToArchive)
                if(archive.exists()) archive.delete()
            }
        }

        showBalloon(title, content, arrayListOf(revealAction, deleteAction))
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

        showBalloon(title, content, arrayListOf(action))
    }

    private fun showBalloon(title: String, content: String = "", actions: ArrayList<AnAction> = arrayListOf()) {
        notification = notificationGroup.createNotification(title, content, NotificationType.INFORMATION)
        if(actions.isNotEmpty()){
            for (action in actions) {
                notification!!.addAction(action)
            }
        }
        Notifications.Bus.notify(notification!!)
    }
}