package net.nemerosa.ontrack.graphql.schema.menu

import graphql.schema.GraphQLObjectType
import net.nemerosa.ontrack.graphql.schema.GQLType
import net.nemerosa.ontrack.graphql.schema.GQLTypeCache
import net.nemerosa.ontrack.graphql.support.*
import net.nemerosa.ontrack.model.ui.UIPage
import org.springframework.stereotype.Component

@Component
class GQLTypeUIPage : GQLType {

    override fun getTypeName(): String = UI_PAGE

    override fun createType(cache: GQLTypeCache): GraphQLObjectType =
            GraphQLObjectType.newObject()
                    .name(UI_PAGE)
                    .description("Defines a page at the front-end.")
                    .field(objectField(UIPage::feature))
                    .field(stringField(UIPage::type))
                    .field(
                            listField(
                                    name = getPropertyName(UIPage::params),
                                    description = getPropertyDescription(UIPage::params)
                            ) { UIPageParam.toUIPageParams(it.getSource()) }
                    )
                    .build()

    companion object {
        val UI_PAGE: String = UIPage::class.java.simpleName
    }

}