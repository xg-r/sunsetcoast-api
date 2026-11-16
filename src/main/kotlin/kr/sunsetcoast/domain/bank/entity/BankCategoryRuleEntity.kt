package kr.sunsetcoast.domain.bank.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import kr.sunsetcoast.domain.bank.vo.BankCategory
import java.time.LocalDateTime

/** 관리자가 만든 분류 규칙 (거래 분류를 고칠 때 '같은 거래내용 모두'를 고르면 생긴다) */
@Entity
@Table(name = "bank_category_rules")
class BankCategoryRuleEntity(
    @Column(nullable = false, length = 100)
    val keyword: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val direction: BankCategorizer.Direction,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: BankCategory,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun toRule() = BankCategorizer.Rule(keyword, direction, category)
}
