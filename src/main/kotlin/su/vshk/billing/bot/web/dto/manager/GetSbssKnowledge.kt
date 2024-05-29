package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty

data class GetSbssKnowledgeRequest(
    @JsonProperty("id")
    val id: Long
)

data class GetSbssKnowledgeResponse(
    @JsonProperty("ret")
    val ret: GetSbssKnowledgeRet? = null
)

data class GetSbssKnowledgeRet(
    @JsonProperty("posts")
    val posts: List<GetSbssKnowledgePostFull>? = null
)

data class GetSbssKnowledgePostFull(
    @JsonProperty("post")
    val post: GetSbssKnowledgePost? = null
)

data class GetSbssKnowledgePost(
    @JsonProperty("text")
    val text: String? = null
)