package net.nemerosa.ontrack.kdsl.model

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.kdsl.client.OntrackConnector
import net.nemerosa.ontrack.kdsl.core.description
import net.nemerosa.ontrack.kdsl.core.name

class KDSLValidationStamp(json: JsonNode, ontrackConnector: OntrackConnector) : KDSLProjectEntity(json, ontrackConnector), net.nemerosa.ontrack.dsl.ValidationStamp {

    override val entityType: String = "VALIDATION_STAMP"
    override val name: String = json.name
    override val description: String = json.description

}