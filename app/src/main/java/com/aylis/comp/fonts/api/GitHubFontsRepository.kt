package com.aylis.comp.fonts.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object GitHubFontsRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: GitHubFontsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubFontsApi::class.java)
    }

    suspend fun searchFonts(query: String): List<FontModel> = withContext(Dispatchers.IO) {
        val fonts = mutableListOf<FontModel>()
        try {
            // Append topic:font to ensure we get font repositories
            val searchQuery = if (query.isBlank()) "font sort:stars" else "$query font"
            val reposResponse = api.searchRepositories(searchQuery)
            
            for (repo in reposResponse.items) {
                try {
                    val treeResponse = api.getRepositoryTree(
                        owner = repo.owner.login,
                        repo = repo.name,
                        branch = repo.default_branch
                    )
                    
                    val fontFiles = treeResponse.tree.filter { 
                        it.type == "blob" && (it.path.endsWith(".ttf", ignoreCase = true) || it.path.endsWith(".otf", ignoreCase = true)) 
                    }

                    for (file in fontFiles) {
                        // Extract just the filename for display
                        val filename = file.path.substringAfterLast("/")
                        val fontName = filename.substringBeforeLast(".")
                            .replace("-", " ")
                            .replace("_", " ")

                        fonts.add(
                            FontModel(
                                name = fontName,
                                author = repo.owner.login,
                                downloadUrl = "https://raw.githubusercontent.com/${repo.owner.login}/${repo.name}/${repo.default_branch}/${file.path}",
                                repositoryName = repo.name
                            )
                        )
                        // Limit to some reasonable amount per repo so we don't get flooded if a repo has 1000 fonts
                        if (fonts.size >= 50) break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue to next repo if one fails (e.g. tree too large or branch issue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fonts.shuffled() // Shuffle to provide diverse results across repos
    }
}
