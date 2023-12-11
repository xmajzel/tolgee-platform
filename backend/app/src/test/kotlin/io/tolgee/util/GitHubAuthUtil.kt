package io.tolgee.util

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.security.third_party.GithubOAuthDelegate
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.client.RestTemplate

class GitHubAuthUtil(
  private val tolgeeProperties: TolgeeProperties,
  private var mockMvc: MockMvc,
  private val restTemplate: RestTemplate
) {
  private val defaultEmailResponse: GithubOAuthDelegate.GithubEmailResponse
    get() {
      val githubEmailResponse = GithubOAuthDelegate.GithubEmailResponse()
      githubEmailResponse.email = "fake_email@email.com"
      githubEmailResponse.primary = true
      githubEmailResponse.verified = true
      return githubEmailResponse
    }

  private val defaultUserResponse: GithubOAuthDelegate.GithubUserResponse
    get() {
      val fakeGithubUser = GithubOAuthDelegate.GithubUserResponse()
      fakeGithubUser.id = "fakeId"
      fakeGithubUser.name = "fakeName"
      return fakeGithubUser
    }

  private val defaultTokenResponse: Map<String, String?>
    get() {
      val accessToken = "fake_access_token"
      val tokenResponse: MutableMap<String, String?> = HashMap()
      tokenResponse["access_token"] = accessToken
      return tokenResponse
    }

  fun authorizeGithubUser(
    tokenResponse: Map<String, String?>? = this.defaultTokenResponse,
    userResponse: ResponseEntity<GithubOAuthDelegate.GithubUserResponse> = ResponseEntity(
      this.defaultUserResponse,
      HttpStatus.OK
    ),
    emailResponse: ResponseEntity<Array<GithubOAuthDelegate.GithubEmailResponse>> = ResponseEntity(
      arrayOf(this.defaultEmailResponse),
      HttpStatus.OK
    )
  ): MvcResult {
    val receivedCode = "ThiS_Is_Fake_valid_COde"
    val githubConf = tolgeeProperties.authentication.github

    doReturn(tokenResponse).whenever(restTemplate)!!
      .postForObject<Map<*, *>>(eq(githubConf.authorizationUrl), any(), any())

    doReturn(userResponse).whenever(restTemplate).exchange(
      eq(githubConf.userUrl),
      eq(HttpMethod.GET),
      any(),
      eq(GithubOAuthDelegate.GithubUserResponse::class.java)
    )

    doReturn(emailResponse).whenever(
      restTemplate
    ).exchange(
      eq(value = githubConf.userUrl + "/emails"),
      eq(HttpMethod.GET),
      any(),
      eq(Array<GithubOAuthDelegate.GithubEmailResponse>::class.java)
    )

    return mockMvc.perform(
      MockMvcRequestBuilders.get("/api/public/authorize_oauth/github?code=$receivedCode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andReturn()
  }
}
