package com.aylis.comp.fonts.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class GitHubRepoSearchResponse(
    val items: List<GitHubRepoItem>
)

data class GitHubRepoItem(
    val name: String,
    val owner: GitHubOwner,
    val default_branch: String
)

data class GitHubOwner(
    val login: String
)

data class GitHubTreeResponse(
    val tree: List<GitHubTreeItem>
)

data class GitHubTreeItem(
    val path: String,
    val type: String
)

interface GitHubFontsApi {
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 5
    ): GitHubRepoSearchResponse

    @GET("repos/{owner}/{repo}/git/trees/{branch}")
    suspend fun getRepositoryTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Query("recursive") recursive: Int = 1
    ): GitHubTreeResponse
}
