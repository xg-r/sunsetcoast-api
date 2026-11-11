package kr.sunsetcoast.domain.property.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 업소(운영 단위). 예: 선셋코스트 캠핑장, 스테이디앤디풀빌라
 */
@Entity
@Table(name = "properties")
class PropertyEntity(
    @Column(nullable = false, length = 50, unique = true)
    val code: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
