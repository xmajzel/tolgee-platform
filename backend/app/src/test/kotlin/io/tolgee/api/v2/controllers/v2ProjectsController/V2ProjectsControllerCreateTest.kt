package io.tolgee.api.v2.controllers.v2ProjectsController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class V2ProjectsControllerCreateTest : AuthorizedControllerTest() {
  private val languageDTO =
    LanguageRequest(
      "English",
      "Original English",
      "en",
      "\uD83C\uDDEC\uD83C\uDDE7",
    )

  lateinit var createForLanguagesDto: CreateProjectRequest

  @BeforeEach
  fun setup() {
    val base = dbPopulator.createBase("SomeProject", "user")
    userAccount = base.userAccount
    createForLanguagesDto =
      CreateProjectRequest(
        name = "What a project",
        organizationId = base.organization.id,
        languages =
          listOf(
            LanguageRequest(
              name = "English",
              originalName = "English",
              tag = "en",
              flagEmoji = "a",
            ),
            LanguageRequest(
              name = "Czech",
              originalName = "česky",
              tag = "cs",
              flagEmoji = "b",
            ),
          ),
        baseLanguageTag = "cs",
      )
  }

  @Test
  fun createProject() {
    dbPopulator.createBase("test")
    testCreateValidationSizeShort()
    testCreateValidationSizeLong()
    testCreateCorrectRequest()
  }

  @Test
  fun createProjectOrganization() {
    val userAccount = dbPopulator.createUserIfNotExists("testuser")
    val organization = dbPopulator.createOrganization("Test Organization", userAccount)
    loginAsUser("testuser")
    val request =
      CreateProjectRequest("aaa", listOf(languageDTO), organizationId = organization.id, disableIcuPlaceholders = true)
    performAuthPost("/v2/projects", request).andIsOk.andAssertThatJson {
      node("disableIcuPlaceholders").isBoolean.isTrue
      node("id").asNumber().satisfies {
        projectService.get(it.toLong()).let {
          assertThat(it.organizationOwner.id).isEqualTo(organization.id)
        }
      }
    }
  }

  @Test
  fun testCreateValidationEmptyLanguages() {
    val request =
      CreateProjectRequest(
        "A name",
        listOf(),
      )
    performAuthPost("/v2/projects", request).andIsBadRequest
  }

  @Test
  fun `validates languages`() {
    val request =
      CreateProjectRequest(
        "A name",
        listOf(
          LanguageRequest(
            name = "English",
            originalName = "English",
            tag = "en,aa",
            flagEmoji = "a",
          ),
        ),
      )
    performAuthPost("/v2/projects", request)
      .andPrettyPrint
      .andIsBadRequest
      .andAssertError.isStandardValidation.onField("languages[0].tag").isEqualTo("can not contain coma")
  }

  private fun testCreateCorrectRequest() {
    val organization = dbPopulator.createOrganizationIfNotExist("nice", userAccount = userAccount!!)
    val request = CreateProjectRequest("aaa", listOf(languageDTO), organizationId = organization.id)
    mvc.perform(
      AuthorizedRequestFactory.loggedPost("/v2/projects")
        .contentType(MediaType.APPLICATION_JSON).content(
          jacksonObjectMapper().writeValueAsString(request),
        ),
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andReturn()
    val projectDto = projectService.findAllPermitted(userAccount!!).find { it.name == "aaa" }
    assertThat(projectDto).isNotNull
    val project = projectService.get(projectDto!!.id!!)
    assertThat(project.languages).isNotEmpty
    val language = project.languages.stream().findFirst().orElse(null)
    assertThat(language).isNotNull
    assertThat(language.tag).isEqualTo("en")
    assertThat(language.name).isEqualTo("English")
    assertThat(language.originalName).isEqualTo("Original English")
    assertThat(language.flagEmoji).isEqualTo("\uD83C\uDDEC\uD83C\uDDE7")
  }

  private fun testCreateValidationSizeShort() {
    val request = CreateProjectRequest("aa", listOf(languageDTO))
    val mvcResult = performAuthPost("/v2/projects", request).andIsBadRequest.andReturn()
    assertThat(mvcResult).error().isStandardValidation
  }

  private fun testCreateValidationSizeLong() {
    val request =
      CreateProjectRequest(
        "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...",
        listOf(languageDTO),
      )
    val mvcResult = performAuthPost("/v2/projects", request).andIsBadRequest.andReturn()
    assertThat(mvcResult).error().isStandardValidation
  }

  @Test
  fun `sets proper baseLanguage on create when provided`() {
    performAuthPost("/v2/projects", createForLanguagesDto).andPrettyPrint.andIsOk.andAssertThatJson {
      node("id").asNumber().satisfies {
        assertThat(projectService.get(it.toLong()).baseLanguage!!.tag)!!.isEqualTo("cs")
      }
    }
  }

  @Test
  fun `sets proper baseLanguage on create when not provided`() {
    performAuthPost("/v2/projects", createForLanguagesDto.copy(baseLanguageTag = null))
      .andIsOk.andAssertThatJson {
        node("id").asNumber().satisfies {
          assertThat(projectService.get(it.toLong()).baseLanguage!!.tag)!!
            .isEqualTo("en")
        }
      }
  }

  @Test
  fun `sets proper baseLanguage on create when not exists`() {
    performAuthPost("/v2/projects", createForLanguagesDto.apply { this.baseLanguageTag = "not_exists" })
      .andIsBadRequest
  }
}
