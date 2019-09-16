package net.nemerosa.ontrack.extension.sonarqube.property

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.extension.sonarqube.SonarQubeExtensionFeature
import net.nemerosa.ontrack.extension.sonarqube.configuration.SonarQubeConfiguration
import net.nemerosa.ontrack.extension.sonarqube.configuration.SonarQubeConfigurationService
import net.nemerosa.ontrack.extension.support.AbstractPropertyType
import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.model.form.*
import net.nemerosa.ontrack.model.security.ProjectConfig
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.ProjectEntity
import net.nemerosa.ontrack.model.structure.ProjectEntityType
import net.nemerosa.ontrack.model.support.ConfigurationPropertyType
import org.springframework.stereotype.Component
import java.util.function.Function

@Component
class SonarQubePropertyType(
        extensionFeature: SonarQubeExtensionFeature,
        private val configurationService: SonarQubeConfigurationService
) : AbstractPropertyType<SonarQubeProperty>(extensionFeature), ConfigurationPropertyType<SonarQubeConfiguration, SonarQubeProperty> {

    override fun getName(): String = "SonarQube"

    override fun getDescription(): String = "Association with a SonarQube project."

    override fun getSupportedEntityTypes(): Set<ProjectEntityType> =
            setOf(ProjectEntityType.PROJECT)

    override fun canEdit(entity: ProjectEntity, securityService: SecurityService): Boolean =
            securityService.isProjectFunctionGranted(entity, ProjectConfig::class.java)

    override fun canView(entity: ProjectEntity, securityService: SecurityService): Boolean = true

    override fun getEditionForm(entity: ProjectEntity?, value: SonarQubeProperty?): Form {
        return Form.create()
                .with(
                        Selection.of("configuration")
                                .label("Configuration")
                                .help("SonarQube configuration to use")
                                .items(configurationService.configurationDescriptors)
                                .value(value?.configuration?.name)
                )
                .with(
                        Text.of("key")
                                .label("Project key")
                                .help("Key of the project in SonarQube")
                                .value(value?.key)
                )
                .with(
                        Text.of("validationStamp")
                                .label("Validation stamp")
                                .help("Validation stamp to listen to for collecting SionarQube metrics on validation run")
                                .value(value?.validationStamp ?: "sonarqube")
                )
                .with(
                        MultiStrings.of("measures")
                                .help("List of SonarQube measures to export.")
                                .label("Measures")
                                .value(value?.measures ?: emptyList<String>())
                )
                .with(
                        YesNo.of("override")
                                .help("Overriding the global settings for the list of SonarQube measures to export.")
                                .label("Override")
                                .value(value?.override ?: false)
                )
    }

    override fun fromClient(node: JsonNode): SonarQubeProperty = fromStorage(node)

    override fun fromStorage(node: JsonNode): SonarQubeProperty {
        val configurationName = node.path("configuration").asText()
        // Looks the configuration up
        val configuration = configurationService.getConfiguration(configurationName)
        // OK
        return SonarQubeProperty(
                configuration,
                node.path("key").asText(),
                node.path("validationStamp").asText(),
                node.path("measures").map { it.asText() },
                node.path("override").asBoolean()
        )
    }

    override fun forStorage(value: SonarQubeProperty): JsonNode =
            mapOf(
                    "configuration" to value.configuration.name,
                    "key" to value.key,
                    "validationStamp" to value.validationStamp,
                    "measures" to value.measures,
                    "override" to value.override
            ).asJson()

    override fun replaceValue(value: SonarQubeProperty, replacementFunction: Function<String, String>) = SonarQubeProperty(
            configurationService.replaceConfiguration(value.configuration, replacementFunction),
            replacementFunction.apply(value.key),
            replacementFunction.apply(value.validationStamp),
            value.measures,
            value.override
    )
}