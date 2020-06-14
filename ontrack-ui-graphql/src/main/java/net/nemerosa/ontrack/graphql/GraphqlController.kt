package net.nemerosa.ontrack.graphql

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import graphql.ExecutionResult
import net.nemerosa.ontrack.common.UserException
import net.nemerosa.ontrack.graphql.service.GraphQLService
import net.nemerosa.ontrack.json.ObjectMapperFactory
import net.nemerosa.ontrack.json.isNullOrNullNode
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.concurrent.Callable

@Transactional(
        noRollbackFor = [UserException::class]
)
@RestController
@RequestMapping("/graphql")
class GraphqlController(
        private val graphQLService: GraphQLService
) {

    private val objectMapper = ObjectMapperFactory.create()

    /**
     * Request model
     */
    data class Request(
            val query: String,
            val variables: Map<String, Any>? = null,
            val operationName: String? = null
    )

    /**
     * GET end point
     */
    @GetMapping
    fun get(
            @RequestParam query: String,
            @RequestParam(required = false) variables: String?,
            @RequestParam(required = false) operationName: String?
    ): Callable<JsonNode> {
        // Parses the arguments
        val arguments = decodeIntoMap(variables)
        // Runs the query
        return Callable {
            requestAsJson(
                    Request(
                            query,
                            arguments,
                            operationName
                    )
            )
        }
    }

    /**
     * POST end point
     */
    @PostMapping
    fun post(@RequestBody input: String): Callable<JsonNode> {
        // Gets the components
        val request = objectMapper.readValue(input, Request::class.java)
        // Runs the query
        return Callable {
            requestAsJson(request)
        }
    }

    /**
     * Request execution (JSON)
     */
    private fun requestAsJson(request: Request): JsonNode {
        return executionResultToJson(request(request))
    }

    private fun executionResultToJson(result: ExecutionResult): JsonNode {
        val json = objectMapper.valueToTree<JsonNode>(result)
        if (json is ObjectNode) {
            val errors = json.path("errors")
            if (errors.isArray && errors.isEmpty) {
                json.remove("errors")
            }
        }
        return json
    }

    /**
     * Request execution
     */
    fun request(request: Request): ExecutionResult {
        return graphQLService.execute(
                request.query,
                request.variables ?: emptyMap(),
                request.operationName,
                true
        )
    }

    @Throws(IOException::class)
    private fun decodeIntoMap(variablesParam: String?): Map<String, Any> =
            (if (variablesParam.isNullOrBlank()) {
                emptyMap()
            } else {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(variablesParam, Map::class.java) as Map<String, Any>
            })

}
