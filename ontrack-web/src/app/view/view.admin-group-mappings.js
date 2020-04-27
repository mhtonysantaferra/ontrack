angular.module('ot.view.admin-group-mappings', [
    'ui.router',
    'ot.service.core',
    'ot.service.graphql'
])
    .config(function ($stateProvider) {
        $stateProvider.state('admin-group-mappings', {
            url: '/admin-group-mappings',
            templateUrl: 'app/view/view.admin-group-mappings.tpl.html',
            controller: 'AdminGroupMappingsCtrl'
        });
    })

    .controller('AdminGroupMappingsCtrl', function ($scope, $http, ot, otGraphqlService) {
        let view = ot.view();
        view.title = "Account group mappings";
        view.description = "Allows to link some groups managed by Ontrack to some groups made available by external authentication providers.";
        view.commands = [
            ot.viewCloseCommand('/admin-accounts')
        ];

        let query = `
            query Mappings {
                authenticationSourceProviders(groupMappingSupported: true) {
                    enabled
                    source {
                      id
                      name
                      allowingPasswordChange
                      groupMappingSupported
                    }
                  }
                  accountGroupMappings {
                    id
                    name
                    group {
                      id
                      name
                      description
                    }
                    type
                  }
            }
        `;

        let queryVariables = {};

        let loadMappings = () => {
            otGraphqlService.pageGraphQLCall(query, queryVariables).then((data) => {
                $scope.mappings = data.accountGroupMappings;
            });
        };

        loadMappings();
    })
;