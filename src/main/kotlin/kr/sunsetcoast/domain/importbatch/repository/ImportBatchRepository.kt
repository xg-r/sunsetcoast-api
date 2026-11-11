package kr.sunsetcoast.domain.importbatch.repository

import kr.sunsetcoast.domain.importbatch.entity.ImportBatchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ImportBatchRepository : JpaRepository<ImportBatchEntity, Long> {
    fun findByFileHash(fileHash: String): ImportBatchEntity?
}
