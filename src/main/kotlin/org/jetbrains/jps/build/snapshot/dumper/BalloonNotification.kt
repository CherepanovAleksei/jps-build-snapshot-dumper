package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File

class BalloonNotification(var project: Project) {
    private val title = "New prebuilt version of master is ready to use"
    private val displayId = "KotlinJpsBuildSnapshotDumper"
    private val notificationGroup = NotificationGroup(displayId, NotificationDisplayType.BALLOON, true)

    fun showBalloon(pathToArchive: String) {
        val notification = notificationGroup.createNotification(title, pathToArchive, NotificationType.INFORMATION)
        notification.addAction(object : AnAction("Delete archive", "", null) {
            override fun actionPerformed(e: AnActionEvent) {
                notification.expire()
                val archive = File(pathToArchive)
                if(archive.exists()) archive.delete()
            }
        })
        Notifications.Bus.notify(notification)
    }
}