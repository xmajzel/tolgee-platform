package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.TagKeysRequest
import io.tolgee.model.batch.params.TagKeysParams
import io.tolgee.service.key.TagService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class TagKeysChunkProcessor(
  private val entityManager: EntityManager,
  private val tagService: TagService
) : ChunkProcessor<TagKeysRequest, TagKeysParams> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: ((Int) -> Unit)
  ) {
    val subChunked = chunk.chunked(100)
    var progress: Int = 0
    var params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      tagService.tagKeysById(subChunk.associateWith { params.tags })
      entityManager.flush()
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  override fun getTarget(data: TagKeysRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<TagKeysParams> {
    return TagKeysParams::class.java
  }

  override fun getParams(data: TagKeysRequest): TagKeysParams {
    return TagKeysParams().apply {
      this.tags = data.tags
    }
  }
}
