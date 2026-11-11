package kr.sunsetcoast.domain.importbatch.repository

import kr.sunsetcoast.domain.importbatch.entity.ImportRowEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ImportRowRepository : JpaRepository<ImportRowEntity, Long>
