package kr.sunsetcoast.domain.importjob.service

import kr.sunsetcoast.domain.importjob.entity.ImportJobEntity
import kr.sunsetcoast.domain.importjob.repository.ImportJobRepository
import kr.sunsetcoast.domain.importjob.vo.ImportJobKind
import kr.sunsetcoast.domain.importjob.vo.ImportJobStatus
import kr.sunsetcoast.domain.notification.entity.NotificationEntity
import kr.sunsetcoast.domain.notification.service.NotificationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

/** 업로드 작업 저장·조회와 상태 변경. 실제 처리는 [ImportJobWorker] */
@Service
@Transactional(readOnly = true)
class ImportJobService(
    private val jobRepository: ImportJobRepository,
    private val notificationService: NotificationService,
) {
    data class Enqueued(val job: ImportJobEntity?, val fileName: String, val rejectedReason: String?)

    /** 처리할 파일 내용 */
    data class Work(val id: Long, val kind: ImportJobKind, val fileName: String, val bytes: ByteArray, val businessOwnerId: Long?)

    /** 파일마다 작업을 만든다. 같은 파일이 이미 대기·처리 중이면 만들지 않는다 */
    @Transactional
    fun create(kind: ImportJobKind, files: List<Pair<String, ByteArray>>, userId: Long?, businessOwnerId: Long? = null): List<Enqueued> {
        require(kind != ImportJobKind.BANK || businessOwnerId != null) { "어느 사업자의 계좌인지 골라 주세요." }
        val seen = mutableSetOf<String>()
        return files.map { (name, bytes) ->
            val hash = sha256(bytes)
            when {
                bytes.isEmpty() -> Enqueued(null, name, "빈 파일입니다.")
                !seen.add(hash) -> Enqueued(null, name, "같은 파일을 두 번 골랐습니다.")
                jobRepository.existsByFileHashAndStatusIn(hash, ACTIVE) -> Enqueued(null, name, "같은 파일이 이미 처리 대기 중입니다.")
                else -> Enqueued(
                    jobRepository.save(ImportJobEntity(kind = kind, businessOwnerId = businessOwnerId, fileName = name, fileHash = hash, fileBytes = bytes, requestedBy = userId)),
                    name,
                    null,
                )
            }
        }
    }

    fun find(ids: Collection<Long>, userId: Long?, canSeeAll: Boolean): List<ImportJobEntity> =
        jobRepository.findAllById(ids).filter { canSeeAll || it.requestedBy == userId }.sortedBy { it.id }

    fun recent(userId: Long?, canSeeAll: Boolean): List<ImportJobEntity> =
        if (canSeeAll || userId == null) jobRepository.findTop50ByOrderByIdDesc() else jobRepository.findTop50ByRequestedByOrderByIdDesc(userId)

    /** 대기 중인 작업을 처리 중으로 바꾸고 파일 내용을 돌려준다. 이미 다른 데서 가져갔으면 null */
    @Transactional
    fun start(id: Long): Work? {
        val job = jobRepository.findById(id).orElse(null) ?: return null
        if (job.status != ImportJobStatus.QUEUED) return null
        val bytes = job.fileBytes ?: run {
            job.fail("파일 내용이 없습니다.")
            return null
        }
        job.start()
        return Work(id, job.kind, job.fileName, bytes, job.businessOwnerId)
    }

    @Transactional
    fun succeed(id: Long, resultJson: String, warning: String?) {
        val job = jobRepository.findById(id).orElseThrow()
        job.succeed(resultJson)
        if (warning != null) notifyRequester(job, NotificationEntity.Level.WARN, "${job.kind.label} 처리 중 확인할 행이 있습니다: ${job.fileName}", warning)
    }

    @Transactional
    fun fail(id: Long, message: String, resultJson: String? = null) {
        val job = jobRepository.findById(id).orElseThrow()
        job.fail(message, resultJson)
        notifyRequester(job, NotificationEntity.Level.ERROR, "${job.kind.label} 업로드 실패: ${job.fileName}", message)
    }

    /** 서버가 멈추기 전에 대기·처리 중이던 작업. 처리 중이던 것은 대기로 되돌린다 (처리는 한 트랜잭션이라 롤백돼 있다) */
    @Transactional
    fun recoverUnfinished(): List<Long> =
        jobRepository.findByStatusInOrderByIdAsc(ACTIVE).onEach { if (it.status == ImportJobStatus.RUNNING) it.requeue() }.map { it.id!! }

    private fun notifyRequester(job: ImportJobEntity, level: NotificationEntity.Level, title: String, message: String) {
        val userId = job.requestedBy ?: return
        notificationService.notify(userId, level, title, message, job.kind.link)
    }

    companion object {
        private val ACTIVE = listOf(ImportJobStatus.QUEUED, ImportJobStatus.RUNNING)

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
