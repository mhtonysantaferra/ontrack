package net.nemerosa.ontrack.boot.graphql

import graphql.GraphQLException
import net.nemerosa.ontrack.model.security.*
import net.nemerosa.ontrack.model.structure.NameDescription
import net.nemerosa.ontrack.test.TestUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class AdminQLIT extends AbstractQLITSupport {

    @Autowired
    private AccountService accountService

    @Test(expected = GraphQLException)
    void 'List of groups needs authorisation'() {
        run("""{ accountGroups { id } }""")
    }

    @Test(expected = GraphQLException)
    void 'List of accounts needs authorisation'() {
        run("""{ accounts { id } }""")
    }

    @Test
    void 'List of groups'() {
        asUser().with(AccountGroupManagement).call {
            def g = accountService.createGroup(NameDescription.nd(TestUtils.uid('G'), '')).id()
            def data = run("""{ accountGroups { id name } }""")
            assert data.accountGroups.find { it.id == g } != null
        }
    }

    @Test
    void 'List of accounts'() {
        def a = doCreateAccount()
        asUser().with(AccountManagement).call {
            def data = run("""{ accounts { id } }""")
            assert data.accounts.find { it.id == a.id() } != null
        }
    }

    @Test
    void 'Account by ID'() {
        def a = doCreateAccount()
        asUser().with(AccountManagement).call {
            def data = run("""{ accounts(id: ${a.id}) { name } }""")
            assert data.accounts.first().name == a.name
        }
    }

    @Test
    void 'Account by name'() {
        def a = doCreateAccount()
        asUser().with(AccountManagement).call {
            def data = run("""{ accounts(name: "${a.name.substring(1)}") { id } }""")
            assert data.accounts.first().id == a.id()
        }
    }

    @Test
    void 'Account by group'() {
        def g = doCreateAccountGroup()
        def a = doCreateAccount()
        asUser().with(AccountManagement).call {
            a = accountService.updateAccount(a.id, new AccountInput(
                    a.name,
                    a.fullName,
                    a.email,
                    '',
                    [g.id()]
            ))
            def data = run("""{ accounts(group: "${g.name}") { id }}""")
            assert data.accounts.first().id == a.id()
        }
    }

    @Test
    void 'Account groups'() {
        def g1 = doCreateAccountGroup()
        def g2 = doCreateAccountGroup()
        def a = doCreateAccount()
        asUser().with(AccountManagement).call {
            a = accountService.updateAccount(a.id, new AccountInput(
                    a.name,
                    a.fullName,
                    a.email,
                    '',
                    [g1.id(), g2.id()]
            ))
            def data = run("""{ accounts(id: ${a.id}) { groups { name } } }""")
            assert data.accounts.first().groups.name == [g1.name, g2.name]
        }
    }

    @Test
    void 'Account without global role'() {
        def a = doCreateAccount()
        def data = asUser().with(AccountManagement).call {
            run("""{
                accounts(id:${a.id}) {
                    globalRole {
                        id name
                    }
                }
            }""")
        }
        assert data.accounts.first().globalRole == null
    }

    @Test
    void 'Account global role'() {
        def a = doCreateAccountWithGlobalRole('CONTROLLER')
        def data = asUser().with(AccountManagement).call {
            run("""{
                accounts(id:${a.id}) {
                    globalRole {
                        id name
                    }
                }
            }""")
        }
        assert data.accounts.first().globalRole.id == 'CONTROLLER'
        assert data.accounts.first().globalRole.name == 'Controller'
    }

    @Test
    void 'Account authorized projects'() {
        def p1 = doCreateProject()
        def p2 = doCreateProject()
        def a = doCreateAccount()
        def data = asAdmin().call {
            accountService.saveProjectPermission(
                    p1.id,
                    PermissionTargetType.ACCOUNT,
                    a.id(),
                    new PermissionInput("PARTICIPANT")
            )
            accountService.saveProjectPermission(
                    p2.id,
                    PermissionTargetType.ACCOUNT,
                    a.id(),
                    new PermissionInput("OWNER")
            )
            return run("""{
                accounts(id: ${a.id}) {
                    authorizedProjects {
                        role {
                            id
                        }
                        project {
                            name
                        }
                    }
                }
            }""")
        }
        def projects = data.accounts.first().authorizedProjects
        assert projects.size() == 2

        assert projects.get(0).role.id == "PARTICIPANT"
        assert projects.get(0).project.name == p1.name

        assert projects.get(1).role.id == "OWNER"
        assert projects.get(1).project.name == p2.name
    }

}