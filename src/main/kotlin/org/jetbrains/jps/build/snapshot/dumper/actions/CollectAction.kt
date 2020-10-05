package org.jetbrains.jps.build.snapshot.dumper.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.jps.build.snapshot.dumper.Collector

class CollectAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Collector(e.project!!).collect()
    }
}