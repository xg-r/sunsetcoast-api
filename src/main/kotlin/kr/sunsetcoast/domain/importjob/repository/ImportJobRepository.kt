package kr.sunsetcoast.domain.importjob.repository

import kr.sunsetcoast.domain.importjob.entity.ImportJobEntity
import kr.sunsetcoast.domain.importjob.vo.ImportJobStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ImportJobRepository : JpaRepository<ImportJobEntity, Long> {
    fun findByStatusInOrderByIdAsc(statuses: Collection<ImportJobStatus>): List<ImportJobEntity>

    fun existsByFileHashAndStatusIn(fileHash: String, statuses: Collection<ImportJobStatus>): Boolean

    fun findTop50ByRequestedByOrderByIdDesc(requestedBy: Long): List<ImportJobEntity>

    fun findTop50ByOrderByIdDesc(): List<ImportJobEntity>
}
