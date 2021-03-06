package net.nemerosa.ontrack.extension.svn.model

import net.nemerosa.ontrack.extension.scm.model.SCMChangeLogCommit
import net.nemerosa.ontrack.extension.scm.model.SCMChangeLogCommits

class SVNChangeLogRevisions(
        val list: List<SVNChangeLogRevision>
) : SCMChangeLogCommits {

    override fun getCommits(): List<SCMChangeLogCommit> = list

    companion object {
        @JvmStatic
        fun none(): SVNChangeLogRevisions {
            return SVNChangeLogRevisions(emptyList<SVNChangeLogRevision>())
        }
    }
}
