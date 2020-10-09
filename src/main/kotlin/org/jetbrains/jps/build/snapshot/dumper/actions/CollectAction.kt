package org.jetbrains.jps.build.snapshot.dumper.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.jps.build.snapshot.dumper.BalloonNotification

class CollectAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        //TODO: extension point
        val project = event.project
        if (project != null) {
            BalloonNotification().showCollectBalloon(project)
        } else {
            BalloonNotification().showNoProjectBalloon()
        }
    }
}