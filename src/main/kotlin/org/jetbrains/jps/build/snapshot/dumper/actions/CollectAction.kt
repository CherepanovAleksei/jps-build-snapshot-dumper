package org.jetbrains.jps.build.snapshot.dumper.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.jps.build.snapshot.dumper.BalloonNotification
import org.jetbrains.jps.build.snapshot.dumper.CollectDialog

class CollectAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project

        if (project != null) {
            val dialog = CollectDialog(project)
            dialog.show()
        } else {
            BalloonNotification().showNoProjectBalloon()
        }
    }
}