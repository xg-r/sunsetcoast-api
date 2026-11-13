package kr.sunsetcoast.domain.importbatch.repository

import kr.sunsetcoast.domain.importbatch.entity.ImportRowEntity
import kr.sunsetcoast.domain.importbatch.vo.ImportRowResult
import org.springframework.data.jpa.repository.JpaRepository

interface ImportRowRepository : JpaRepository<ImportRowEntity, Long> {
    fun findByResultOrderByIdAsc(result: ImportRowResult): List<ImportRowEntity>
}
