package net.nemerosa.ontrack.extension.sonarqube

import net.nemerosa.ontrack.extension.sonarqube.property.SonarQubePropertyType
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.ProjectEntityType
import net.nemerosa.ontrack.model.structure.PropertyService
import net.nemerosa.ontrack.ui.resource.LinkDefinition
import net.nemerosa.ontrack.ui.resource.LinkDefinitions
import net.nemerosa.ontrack.ui.resource.ResourceDecorationContributor
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on

@Component
class SonarQubeBuildResourceDecorationContributor(
        private val propertyService: PropertyService
) : ResourceDecorationContributor<Build> {

    override fun getLinkDefinitions(): List<LinkDefinition<Build>> {
        return listOf(
                LinkDefinitions.link(
                        "_collectSonarQube",
                        { build, _ -> on(SonarQubeController::class.java).collectBuildMeasures(build.id) },
                        { build, _ -> propertyService.hasProperty(build.project, SonarQubePropertyType::class.java) }
                )
        )
    }

    override fun applyTo(projectEntityType: ProjectEntityType): Boolean =
            projectEntityType == ProjectEntityType.BUILD
}