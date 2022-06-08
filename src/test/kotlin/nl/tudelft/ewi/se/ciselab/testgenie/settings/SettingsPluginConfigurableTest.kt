package nl.tudelft.ewi.se.ciselab.testgenie.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.TestFixtureBuilder
import nl.tudelft.ewi.se.ciselab.testgenie.services.SettingsProjectService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsPluginConfigurableTest {
    private var project: Project = Mockito.mock(Project::class.java)
    private lateinit var settingsConfigurable: SettingsPluginConfigurable
    private lateinit var settingsComponent: SettingsPluginComponent
    private lateinit var settingsProjectService: SettingsProjectService
    private var settingsState: SettingsProjectState = SettingsProjectState()
    private lateinit var fixture: CodeInsightTestFixture

    @BeforeEach
    fun setUp() {
        project = Mockito.mock(Project::class.java)
        settingsProjectService = Mockito.mock(SettingsProjectService::class.java)

        settingsConfigurable = SettingsPluginConfigurable(project)

        Mockito.`when`(project.service<SettingsProjectService>()).thenReturn(settingsProjectService)
        Mockito.`when`(settingsProjectService.state).thenReturn(settingsState)

        val projectBuilder: TestFixtureBuilder<IdeaProjectTestFixture> =
            IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("project")

        fixture = JavaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(projectBuilder.fixture)
        fixture.setUp()

        settingsConfigurable.createComponent()
        settingsConfigurable.reset()
        settingsComponent = settingsConfigurable.settingsComponent!!
//        settingsState = ApplicationManager.getApplication().getService(SettingsApplicationService::class.java).state
    }

    @AfterEach
    fun tearDown() {
        fixture.tearDown()
        settingsConfigurable.disposeUIResources()
    }

    @ParameterizedTest
    @MethodSource("intValueGenerator")
    fun testIsModifiedValues(
        oldValue: Int,
        function: () -> Unit,
        component: () -> Int,
        state: () -> Int
    ) {
        function()
        assertThat(settingsConfigurable.isModified).isTrue
        assertThat(component()).isNotEqualTo(oldValue)
        assertThat(state()).isEqualTo(oldValue)
    }

    @ParameterizedTest
    @MethodSource("intValueGenerator")
    fun testApplyValues(oldValue: Int, function: () -> Unit, component: () -> Int, state: () -> Int) {
        function()
        settingsConfigurable.apply()
        assertThat(component()).isNotEqualTo(oldValue)
        assertThat(state()).isNotEqualTo(oldValue)
    }

    @Test
    fun testIsModifiedJavaPath() {
        settingsComponent.javaPath = "this is modified: first"
        assertThat(settingsConfigurable.isModified).isTrue
        assertThat(settingsComponent.javaPath).isNotEqualTo(settingsState.javaPath)
    }

    @Test
    fun testResetJavaPath() {
        val oldValue = settingsComponent.javaPath
        settingsComponent.javaPath = "this is modified: second"
        settingsConfigurable.reset()
        assertThat(oldValue).isEqualTo(settingsComponent.javaPath)
    }

    @Test
    fun testApplyJavaPath() {
        val oldValue = settingsComponent.javaPath
        settingsComponent.javaPath = "this is modified: third"
        settingsConfigurable.apply()
        assertThat(oldValue).isNotEqualTo(settingsComponent.javaPath)
        assertThat(oldValue).isNotEqualTo(settingsState.javaPath)
    }

    private fun intValueGenerator(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                settingsComponent.colorBlue,
                { settingsComponent.colorBlue += 20 },
                { settingsComponent.colorBlue },
                { settingsState.colorBlue }
            ),
            Arguments.of(
                settingsComponent.colorRed,
                { settingsComponent.colorRed += 30 },
                { settingsComponent.colorRed },
                { settingsState.colorRed }
            ),
            Arguments.of(
                settingsComponent.colorGreen,
                { settingsComponent.colorGreen += 10 },
                { settingsComponent.colorGreen },
                { settingsState.colorGreen }
            )
        )
    }
}
