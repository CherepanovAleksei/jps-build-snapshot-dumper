package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

class BalloonNotification(var project: Project) {
    private val title = "New prebuilt version of master is ready to use"
    private val displayId = "KotlinJpsBuildSnapshotDumper"
    private val notificationGroup = NotificationGroup(displayId, NotificationDisplayType.BALLOON, true)

    fun showBalloon(string: String) {
        val notification = notificationGroup.createNotification(title, string, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification)
    }
}