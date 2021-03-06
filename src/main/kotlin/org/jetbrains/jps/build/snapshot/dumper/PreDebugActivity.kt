package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PreDebugActivity : StartupActivity {
    private val LOG = Logger.getInstance("org.jetbrains.jps.build.snapshot.dumper.PreDebugActivity")

    override fun runActivity(project: Project) {
        val dialog = CollectDialog(project)
        dialog.show()
    }
}